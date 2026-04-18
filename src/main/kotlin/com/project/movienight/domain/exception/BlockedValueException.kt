package com.project.movienight.domain.exception

class BlockedValueException(
    target: String,
    field: String,
) : DomainException("$target with this $field is not acceptable")
