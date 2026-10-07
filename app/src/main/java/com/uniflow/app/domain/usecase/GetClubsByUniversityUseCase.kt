package com.uniflow.app.domain.usecase

import com.uniflow.app.core.common.Resource
import com.uniflow.app.domain.model.Club
import com.uniflow.app.domain.repository.UniversityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetClubsByUniversityUseCase @Inject constructor(
    private val repository: UniversityRepository
) {
    operator fun invoke(universityId: String): Flow<Resource<List<Club>>> {
        return repository.getClubsByUniversity(universityId)
    }
}