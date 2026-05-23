#!/usr/bin/env python3

import os
import sys
import urllib.request
import argparse
import logging
import re
import random

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)

DEFAULT_DATASET_URL = "https://raw.githubusercontent.com/sidooms/MovieTweetings/master/latest/movies.dat"
DEFAULT_OUTPUT_DIR = "./Jellyfin_Movies"
DEFAULT_COUNT = 1000

def download_dataset(url):
    logger.info(f"Downloading dataset from {url}...")
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req) as response:
            data = response.read().decode('utf-8')
        logger.info("Dataset downloaded successfully.")
        return data.splitlines()
    except Exception as e:
        logger.error(f"Failed to download dataset: {e}")
        sys.exit(1)

def parse_movies(lines):
    """
    Parse the movies.dat file.
    Format: IMDbID::Title (Year)::Genres
    Example: 0000008::Edison Kinetoscopic Record of a Sneeze (1894)::Documentary|Short
    """
    movies = []
    # Regex to extract Title and Year from "Title (Year)"
    title_year_pattern = re.compile(r'(.*)\s+\((\d{4})\)$')

    for line in lines:
        line = line.strip()
        if not line:
            continue

        parts = line.split('::')
        if len(parts) >= 2:
            imdb_id_raw = parts[0]
            title_year_raw = parts[1]

            # Format IMDb ID to ttXXXXXXX
            if imdb_id_raw.isdigit():
                imdb_id = f"tt{imdb_id_raw.zfill(7)}"
            else:
                continue

            match = title_year_pattern.match(title_year_raw)
            if match:
                title = match.group(1).strip()
                year = match.group(2)

                # Clean title for filesystem (remove invalid characters)
                safe_title = re.sub(r'[\\/*?:"<>|]', "", title)
                safe_title = safe_title.strip()

                if safe_title:
                    movies.append({
                        'imdb_id': imdb_id,
                        'title': safe_title,
                        'year': year
                    })

    logger.info(f"Parsed {len(movies)} valid movies from dataset.")
    return movies

def create_dummy_video(filepath):
    """Create a minimal valid dummy video file (mp4)."""
    try:
        mp4_header = b"\x00\x00\x00\x18ftypmp42\x00\x00\x00\x00mp42isom\x00\x00\x00\x00moov\x00\x00\x00\x08mvhd"
        with open(filepath, 'wb') as f:
            f.write(mp4_header)
        return True
    except Exception as e:
        logger.error(f"Failed to create dummy video {filepath}: {e}")
        return False

def generate_library(movies, output_dir, count):
    """Generate the folder structure and dummy files."""
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
        logger.info(f"Created output directory: {output_dir}")

    generated_imdb_ids = set()
    created_count = 0
    skipped_count = 0
    failed_count = 0

    logger.info(f"Starting generation of up to {count} movies...")

    # Shuffle to get diverse movies
    random.shuffle(movies)

    for movie in movies:
        if created_count >= count:
            break

        if movie['imdb_id'] in generated_imdb_ids:
            skipped_count += 1
            continue

        # Jellyfin naming convention: Movie Name (year) [imdbid-tt1234567]
        folder_name = f"{movie['title']} ({movie['year']}) [imdbid-{movie['imdb_id']}]"
        folder_path = os.path.join(output_dir, folder_name)

        file_name = f"{folder_name}.mp4"
        file_path = os.path.join(folder_path, file_name)

        if os.path.exists(file_path):
            skipped_count += 1
            generated_imdb_ids.add(movie['imdb_id'])
            continue

        try:
            os.makedirs(folder_path, exist_ok=True)
            if create_dummy_video(file_path):
                created_count += 1
                generated_imdb_ids.add(movie['imdb_id'])
            else:
                failed_count += 1
        except Exception as e:
            logger.error(f"Error processing {folder_name}: {e}")
            failed_count += 1

    logger.info("--- Generation Summary ---")
    logger.info(f"Target count: {count}")
    logger.info(f"Successfully created: {created_count}")
    logger.info(f"Skipped (already exists or duplicate): {skipped_count}")
    logger.info(f"Failed: {failed_count}")

    return created_count > 0

def main():
    parser = argparse.ArgumentParser(description="Generate a dummy Jellyfin movie library.")
    parser.add_argument("--output-dir", type=str, default=DEFAULT_OUTPUT_DIR,
                        help=f"Directory to create the library in (default: {DEFAULT_OUTPUT_DIR})")
    parser.add_argument("--count", type=int, default=DEFAULT_COUNT,
                        help=f"Number of movies to generate (default: {DEFAULT_COUNT})")
    parser.add_argument("--dataset-url", type=str, default=DEFAULT_DATASET_URL,
                        help="URL to the movies.dat file")

    args = parser.parse_args()

    lines = download_dataset(args.dataset_url)
    if not lines:
        logger.error("No dataset lines to process.")
        sys.exit(1)

    movies = parse_movies(lines)

    if not movies:
        logger.error("No movies parsed from the dataset.")
        sys.exit(1)

    if len(movies) < args.count:
        logger.warning(f"Requested {args.count} movies, but only {len(movies)} available.")
        args.count = len(movies)

    success = generate_library(movies, args.output_dir, args.count)

    if success:
        logger.info(f"Library generation complete. You can now mount '{os.path.abspath(args.output_dir)}' into Jellyfin.")
    else:
        logger.error("Library generation failed.")
        sys.exit(1)

if __name__ == "__main__":
    main()
