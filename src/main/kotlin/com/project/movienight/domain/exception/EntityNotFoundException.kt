package com.project.movienight.domain.exception

class EntityNotFoundException(
    entity: String,
    id: String,
) : DomainException("$entity with id $id not found")
