package net.korul.hbbft.services

import android.util.Log
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService


class MyJobService : JobService() {

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        Log.d(TAG, "Performing long running task in scheduled job")

//        val latch = CountDownLatch(1)
//        DatabaseApplication.mCoreHBBFT2X.setOfflineModeToDatabase(DatabaseApplication.mCoreHBBFT2X.mRoomName, latch)
//        latch.await()

        return false
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        return false
    }

    companion object {

        private const val TAG = "HYDRABADGERTAG:JobSer"
    }
}