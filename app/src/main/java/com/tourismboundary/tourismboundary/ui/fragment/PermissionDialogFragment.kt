package com.tourismboundary.tourismboundary.ui.fragment


import android.app.Dialog
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tourismboundary.tourismboundary.R
import com.tourismboundary.tourismboundary.safeLet
import com.tourismboundary.tourismboundary.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.fragment_permission_dialog.*

class PermissionDialogFragment : DialogFragment() {
    companion object {
        val TAG = PermissionDialogFragment::class.java.simpleName
    }

    private lateinit var mViewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_permission_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        btnSetting.setOnClickListener {
            val status = arguments?.getInt("REQUEST_CODE")
            safeLet(activity, status) { parent, code ->
                mViewModel.requestPermission(parent,code)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.run {
            mViewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog.window?.attributes?.windowAnimations = R.style.MoveFromBottom
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false
        return super.onCreateDialog(savedInstanceState)
    }


}

