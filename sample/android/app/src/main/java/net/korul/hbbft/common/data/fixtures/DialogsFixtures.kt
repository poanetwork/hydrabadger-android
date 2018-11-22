package net.korul.hbbft.common.data.fixtures

import net.korul.hbbft.common.data.model.Dialog
import net.korul.hbbft.common.data.model.core.Getters.getAllDialog


class DialogsFixtures private constructor() {
    init {
        throw AssertionError()
    }

    companion object {
        val dialogs: ArrayList<Dialog>
            get() {
                return ArrayList(getAllDialog())
            }
    }
}
