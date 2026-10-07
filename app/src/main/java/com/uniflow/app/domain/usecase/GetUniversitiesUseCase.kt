package com.uniflow.app.domain.usecase

import com.uniflow.app.core.common.Resource
import com.uniflow.app.domain.model.University
import com.uniflow.app.domain.repository.UniversityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUniversitiesUseCase @Inject constructor(
    private val repository: UniversityRepository
) {
    operator fun invoke(): Flow<Resource<List<University>>> {
        return repository.getUniversities()
    }
}