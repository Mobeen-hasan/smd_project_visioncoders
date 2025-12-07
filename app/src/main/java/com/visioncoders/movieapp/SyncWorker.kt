package com.visioncoders.movieapp

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.visioncoders.movieapp.ReviewReply
import com.visioncoders.movieapp.UserReview

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.appDao()
        val firestore = FirebaseFirestore.getInstance()
        val gson = Gson()

        // 1. Get all pending actions from SQLite
        val pendingActions = dao.getPendingActions()

        if (pendingActions.isEmpty()) return Result.success()

        for (action in pendingActions) {
            try {
                if (action.type == "REVIEW") {
                    val review = gson.fromJson(action.payload, UserReview::class.java)

                    Tasks.await(firestore.collection("reviews").add(review))
                }
                else if (action.type == "REPLY") {
                    val reply = gson.fromJson(action.payload, ReviewReply::class.java)
                    Tasks.await(
                        firestore.collection("reviews")
                            .document(reply.reviewId)
                            .collection("replies")
                            .add(reply)
                    )
                }

                // 2. If successful, remove from SQLite Queue
                dao.deleteAction(action.id)

            } catch (e: Exception) {
                e.printStackTrace()
                return Result.retry()
            }
        }

        return Result.success()
    }
}