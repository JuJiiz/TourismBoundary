package com.tourismboundary.tourismboundary.ui.fragment


import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.model.LatLng
import com.tourismboundary.tourismboundary.R
import com.tourismboundary.tourismboundary.safeLet
import com.tourismboundary.tourismboundary.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.fragment_send_email.*
import java.text.DecimalFormat

class SendEmailFragment : Fragment() {
    private var locationList: List<LatLng>? = null
    private var dimension: Int? = null

    private lateinit var mViewModel: MainViewModel

    companion object {
        val Tag = SendEmailFragment::class.java.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_send_email, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.also { arg ->
            locationList = arg.getParcelableArrayList("LOCATION_LIST")
            dimension = arg.getInt("DIMENSION")
        }

        val formatter = DecimalFormat("#,###,###")
        tvDimension.text = "${formatter.format(dimension)} ${context?.resources?.getString(R.string.square_meters)}"

        btnSend.setOnClickListener {
            safeLet(locationList, dimension, edtEmail.text, context) { locationList, dimension, email, context ->
                mViewModel.sendToEmail(context, email.toString(), dimension.toString(), locationList)
            }
        }

        btnBack.setOnClickListener {
            fragmentManager?.popBackStack()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.run {
            mViewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        }
    }


}
