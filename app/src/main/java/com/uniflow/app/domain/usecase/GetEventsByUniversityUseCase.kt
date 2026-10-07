package com.uniflow.app.domain.usecase

import com.uniflow.app.core.common.Resource
import com.uniflow.app.domain.model.Event
import com.uniflow.app.domain.repository.UniversityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEventsByUniversityUseCase @Inject constructor(
    private val repository: UniversityRepository
) {
    operator fun invoke(universityId: String): Flow<Resource<List<Event>>> {
        return repository.getEventsByUniversity(universityId)
    }
}