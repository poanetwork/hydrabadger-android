package net.korul.hbbft.services

import android.util.Log
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.raizlabs.android.dbflow.kotlinextensions.delete
import net.korul.hbbft.DatabaseApplication


class MyJobService : JobService() {

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        Log.d(TAG, "Performing long running task in scheduled job")

        Thread.sleep(7*1000)
        DatabaseApplication.mCoreHBBFT2X.freeCoreHBBFT()
        DatabaseApplication.delete()
        Log.i(TAG, "freeCoreHBBFT()")

        return false
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        return false
    }

    companion object {

        private const val TAG = "HYDRA:JobSer"
    }
}