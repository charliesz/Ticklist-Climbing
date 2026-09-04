package com.charlie.ticklist.data

data class ProgressTransferPreview(
    val sourceCollectionId: Int,
    val targetCollectionId: Int,
    val sourceCollectionName: String,
    val targetCollectionName: String,
    val sourceRouteCount: Int,
    val targetRouteCount: Int,
    val matchingRouteCount: Int
) {
    val routeCountsMatch: Boolean
        get() = sourceRouteCount == targetRouteCount

    val canTransfer: Boolean
        get() =
            sourceCollectionId != targetCollectionId &&
                    routeCountsMatch &&
                    matchingRouteCount == sourceRouteCount
}

class ProgressTransferRepository(
    private val collectionDao: CollectionDao,
    private val routeDao: RouteDao
) {

    suspend fun createPreview(
        sourceCollectionId: Int,
        targetCollectionId: Int
    ): ProgressTransferPreview {
        require(sourceCollectionId != targetCollectionId) {
            "Quell- und Zielsammlung müssen unterschiedlich sein."
        }

        val sourceCollection = collectionDao.getCollection(
            sourceCollectionId
        ) ?: error("Quellsammlung nicht gefunden.")

        val targetCollection = collectionDao.getCollection(
            targetCollectionId
        ) ?: error("Zielsammlung nicht gefunden.")

        val sourceRoutes = routeDao.getRoutesForCollection(
            sourceCollectionId
        )

        val targetRoutes = routeDao.getRoutesForCollection(
            targetCollectionId
        )

        val targetNumbers = targetRoutes
            .map { it.number }
            .toSet()

        val matchingRoutes = sourceRoutes.count {
            it.number in targetNumbers
        }

        return ProgressTransferPreview(
            sourceCollectionId = sourceCollectionId,
            targetCollectionId = targetCollectionId,
            sourceCollectionName = sourceCollection.name,
            targetCollectionName = targetCollection.name,
            sourceRouteCount = sourceRoutes.size,
            targetRouteCount = targetRoutes.size,
            matchingRouteCount = matchingRoutes
        )
    }

    suspend fun transferProgress(
        sourceCollectionId: Int,
        targetCollectionId: Int,
        overwriteExistingProgress: Boolean
    ): ProgressTransferPreview {
        val preview = createPreview(
            sourceCollectionId = sourceCollectionId,
            targetCollectionId = targetCollectionId
        )

        require(preview.canTransfer) {
            "Die Sammlungen besitzen nicht dieselbe Routenzahl " +
                    "oder nicht dieselben Routennummern."
        }

        val sourceRoutes = routeDao.getRoutesForCollection(
            sourceCollectionId
        )

        val targetRoutes = routeDao.getRoutesForCollection(
            targetCollectionId
        )

        val targetByNumber = targetRoutes.associateBy {
            it.number
        }

        for (sourceRoute in sourceRoutes) {
            val targetRoute = targetByNumber[sourceRoute.number]
                ?: continue

            val hasTargetProgress =
                targetRoute.status != null ||
                        targetRoute.statusChangedAt != null ||
                        targetRoute.completedDate != null

            if (
                hasTargetProgress &&
                !overwriteExistingProgress
            ) {
                continue
            }

            routeDao.updateRouteWithDates(
                number = targetRoute.number,
                name = targetRoute.name,
                difficulty = targetRoute.difficulty,
                status = sourceRoute.status,
                statusChangedAt = sourceRoute.statusChangedAt,
                completedDate = sourceRoute.completedDate,
                collectionId = targetCollectionId
            )
        }

        return preview
    }
}
