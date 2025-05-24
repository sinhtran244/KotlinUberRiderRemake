package com.example.kotlinuberriderremake.ui.home


import android.os.Handler

import com.google.firebase.database.ValueEventListener
import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import com.example.kotlinuberriderremake.Common.Common
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ChildEventListener
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Address
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.kotlinuberriderremake.Callback.FirebaseDriverInfoListener
import com.example.kotlinuberriderremake.Callback.FirebaseFailedListener
import com.example.kotlinuberriderremake.R
import com.example.kotlinuberriderremake.databinding.FragmentHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.util.Locale
import android.location.Geocoder
import android.text.TextUtils
import android.view.animation.LinearInterpolator
import com.example.kotlinuberriderremake.Model.AnimationMode
import com.example.kotlinuberriderremake.Model.DriverGeoModel
import com.example.kotlinuberriderremake.Model.DriverInfoModel
import com.example.kotlinuberriderremake.Model.GeoQueryModel
import com.example.kotlinuberriderremake.Remote.IGoogleAPI
import com.example.kotlinuberriderremake.Remote.RetrofitClient
import kotlin.text.toInt


import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.sothree.slidinguppanel.SlidingUpPanelLayout

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.json.JSONObject
import java.io.IOException
import java.util.ArrayList


class HomeFragment : Fragment() , OnMapReadyCallback,FirebaseDriverInfoListener, FirebaseFailedListener{
        private lateinit var mMap: GoogleMap
        private var _binding: FragmentHomeBinding? = null
        private lateinit var mapFragment: SupportMapFragment

        private lateinit var slidingUpPanelLayout: SlidingUpPanelLayout
        private lateinit var txt_welcome: TextView
        private lateinit var autocompleteSupportFragment: AutocompleteSupportFragment


        private lateinit var locationRequest: LocationRequest
        private lateinit var locationCallback: LocationCallback
        private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

            // This property is only valid between onCreateView and
            // onDestroyView.
        private val binding get() = _binding!!


        private val childEventListeners = mutableListOf<ChildEventListener>()

        //Load Driver
        var distance = 1.0
        val LIMIT_RANGE = 10.0
        var previousLocation: Location? = null
        var currentLocation: Location? = null

        var firstTime = true

        //Listener
        lateinit var ifirebaseDriverInfoListener: FirebaseDriverInfoListener
        lateinit var ifirebaseFailedListener: FirebaseFailedListener

        var cityName = ""

        //
        val compositeDisposable = CompositeDisposable()
        lateinit var iGoogleAPI: IGoogleAPI



        override fun onStop() {
            compositeDisposable.clear()
            super.onStop()
        }


        override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        geoQuery?.removeAllListeners()
        geoQueryListeners.forEach { geoQuery?.removeGeoQueryEventListener(it) }
        geoQueryListeners.clear()
        Common.driversFound.clear()
        Common.markerList.clear()
        super.onDestroy()
    }




        @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
            ): View {
            val homeViewModel =
                ViewModelProvider(this).get(HomeViewModel::class.java)

            _binding = FragmentHomeBinding.inflate(inflater, container, false)
            val root: View = binding.root

            init()
            initViews(root)

            mapFragment = childFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)
            return root
        }
        @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
        private fun init(){
            // Initialize Places API with Google Maps API key
            Places.initialize(requireContext().getString(R.string.google_maps_key))

            // Get the AutocompleteSupportFragment instance
            val autocompleteSupportFragment = childFragmentManager
                .findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment

            // Set the place fields to return
            autocompleteSupportFragment.setPlaceFields(Arrays.asList(
                Place.Field.ID,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.NAME
            ))

            // Set up place selection listener
            autocompleteSupportFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
                override fun onPlaceSelected(place: Place) {
                    Snackbar.make(requireView(),
                        "Selected: ${place.name}\nLocation: ${place.latLng}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }

                override fun onError(status: Status) {
                    Snackbar.make(requireView(),
                        status.statusMessage ?: "An error occurred",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            })
            iGoogleAPI = RetrofitClient.instance!!.create(IGoogleAPI::class.java)

            Log.d("myTAG", "init() called")
            ifirebaseDriverInfoListener = this
            locationRequest = LocationRequest()
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            locationRequest.setFastestInterval(3000)
            locationRequest.setSmallestDisplacement(10f)
            locationRequest.interval = 5000
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(p0: LocationResult) {
                    super.onLocationResult(p0)
                    val newPos = LatLng(p0!!.lastLocation!!.latitude, p0!!.lastLocation!!.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos, 18f))

                    if (firstTime) {
                        previousLocation = p0.lastLocation
                        currentLocation = p0.lastLocation
                        firstTime = false
                    } else {
                        previousLocation = currentLocation
                        currentLocation = p0.lastLocation
                    }

                    if (previousLocation?.distanceTo(currentLocation!!)?.div(1000)!! <= LIMIT_RANGE) {
                        loadAvailableDrivers()
                    }
                }
            }
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    101
                )
              //  Snackbar.make(requireView(), getString(R.string.permission_require), Snackbar.LENGTH_SHORT).show()
                return

            }

            val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(requireContext(), "Please enable GPS", Toast.LENGTH_LONG).show()
                return
            }

            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
            Log.e("myTAG", "loadAvailableDrivers()")
            loadAvailableDrivers()
        }

    private fun initViews(root: View?) {
        slidingUpPanelLayout = root!!.findViewById<SlidingUpPanelLayout>(R.id.activity_main)
        txt_welcome = root!!.findViewById<TextView>(R.id.txt_welcome)

        Common.setWelcomeMessage(txt_welcome)
    }





    private var geoQuery: GeoQuery? = null
                private val geoQueryListeners = mutableListOf<GeoQueryEventListener>()

    @SuppressLint("RestrictedApi")
    private fun loadAvailableDrivers() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (isAdded && _binding != null) {
                Snackbar.make(requireView(), getString(R.string.permission_require), Snackbar.LENGTH_SHORT).show()
            }
            return
        }
        fusedLocationProviderClient.lastLocation
            .addOnFailureListener { e ->
                Log.e("myTAG", "Failed to get rider location: ${e.message}")
                if (isAdded && _binding != null) {
                    Snackbar.make(requireView(), e.message ?: "Không lấy được vị trí", Snackbar.LENGTH_SHORT).show()
                }
            }
            .addOnSuccessListener { location ->
                Log.d("myTAG", "Rider location: (${location.latitude}, ${location.longitude})")
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                var addressList: List<Address> = emptyList()
                try {
                    @Suppress("DEPRECATION")
                    addressList = geocoder.getFromLocation(location.latitude, location.longitude, 1) ?: emptyList()
                    cityName = when {
                        addressList.isNotEmpty() && addressList[0].locality != null -> addressList[0].locality
                        addressList.isNotEmpty() && addressList[0].adminArea != null -> addressList[0].adminArea
                        addressList.isNotEmpty() && addressList[0].subAdminArea != null -> addressList[0].subAdminArea
                        else -> "Ho Chi Minh"
                    }
                    cityName = cityName.replace("Hồ Chí Minh", "Ho Chi Minh").replace("Thủ Đức", "Thu Duc")
                    Log.d("myTAG", "CityName: $cityName")
                    val driver_location_ref = FirebaseDatabase.getInstance()
                        .getReference(Common.DRIVERS_LOCATION_REFERENCES)
                        .child(cityName)
                    Log.d("myTAG","Path Firebase: ${driver_location_ref.path}")
                    val gf = GeoFire(driver_location_ref)
                    // Xóa GeoQuery cũ
                    geoQuery?.removeAllListeners()
                    geoQueryListeners.forEach { geoQuery?.removeGeoQueryEventListener(it) }
                    geoQueryListeners.clear()
                    // Tạo GeoQuery mới với bán kính 10km
                    geoQuery = gf.queryAtLocation(GeoLocation(location.latitude, location.longitude), 10.0)
                    val geoListener = object : GeoQueryEventListener {
                        override fun onGeoQueryReady() {
                            Log.d("myTAG", "GeoQuery ready, drivers found: ${Common.driversFound.size}")
                            addDriverMarker()
                        }
                        override fun onKeyEntered(key: String?, location: GeoLocation?) {
                            if (key != null && location != null) {
                                Log.d("myTAG", "Driver found: $key at (${location.latitude}, ${location.longitude})")
                                val driverGeoModel = DriverGeoModel(key, location)
                                Common.driversFound.add(driverGeoModel)
                                findDriverByKey(driverGeoModel)
                            } else {
                                Log.e("myTAG", "Invalid key or location in onKeyEntered")
                            }
                        }
                        override fun onKeyMoved(key: String?, location: GeoLocation?) {
                            Log.d("myTAG", "Driver moved: $key to (${location?.latitude}, ${location?.longitude})")
                        }
                        override fun onKeyExited(key: String?) {
                            Log.d("myTAG", "Driver exited: $key")
                            Common.driversFound.removeIf { it.key == key }
                            Common.markerList[key]?.remove()
                            Common.markerList.remove(key)
                        }
                        override fun onGeoQueryError(error: DatabaseError?) {
                            Log.e("myTAG", "GeoQuery error: ${error?.message}")
                            if (isAdded && _binding != null) {
                                Snackbar.make(requireView(), error?.message ?: "Lỗi GeoQuery", Snackbar.LENGTH_SHORT).show()
                            }
                        }
                    }
                    geoQuery?.addGeoQueryEventListener(geoListener)
                    geoQueryListeners.add(geoListener)
                } catch (e: Exception) {
                    Log.e("myTAG", "Geocoder error: ${e.message}")
                    cityName = "Ho Chi Minh"
                    if (isAdded && _binding != null) {
                        Snackbar.make(requireView(), "Không xác định được thành phố, dùng mặc định: Ho Chi Minh", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
    }




    @SuppressLint("CheckResult")
    private fun addDriverMarker() {
        if (Common.driversFound.isNotEmpty()) {
            Observable.fromIterable(Common.driversFound)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { driverGeoModel: DriverGeoModel? ->
                        if (driverGeoModel != null) {
                            findDriverByKey(driverGeoModel)
                        } else {
                            Log.e("myTAG", "DriverGeoModel is null")
                        }
                    },
                    { t: Throwable ->
                        Log.e("myTAG", "Error in addDriverMarker: ${t.message}")
                        Snackbar.make(requireView(), t.message ?: "Lỗi không xác định", Snackbar.LENGTH_SHORT).show()
                    }
                )
        } else {
            Log.d("myTAG", "No drivers found in Common.driversFound")
            Snackbar.make(requireView(), getString(R.string.drivers_not_found), Snackbar.LENGTH_SHORT).show()
        }
    }


    private fun findDriverByKey(driverGeoModel: DriverGeoModel?) {
        Log.e("myTAG", "findDriverByKey() start")
        if (driverGeoModel?.key == null) {
            Log.e("myTAG", "DriverGeoModel or key is null")
            ifirebaseFailedListener.onFirebaseFailed("Driver key is null")
            return
        }
        FirebaseDatabase.getInstance()
            .getReference(Common.DRIVER_INFO_REFERENCE)
            .child(driverGeoModel.key!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {
                    Log.e("myTAG", "Firebase error for key ${driverGeoModel.key}: ${error.message}")
                    ifirebaseFailedListener.onFirebaseFailed(error.message)
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        try {
                            val driverInfo = snapshot.getValue(DriverInfoModel::class.java)
                            if (driverInfo != null) {
                                driverGeoModel.driverInfoModel = driverInfo
                                ifirebaseDriverInfoListener.onDriverInfoLoadSuccess(driverGeoModel)
                                Log.d("myTAG", "DriverInfo loaded for key: ${driverGeoModel.key}")
                            } else {
                                Log.e("myTAG", "DriverInfo is null for key: ${driverGeoModel.key}")
                                ifirebaseFailedListener.onFirebaseFailed("Invalid DriverInfo data for key: ${driverGeoModel.key}")
                            }
                        } catch (e: Exception) {
                            Log.e("myTAG", "Failed to parse DriverInfo for key ${driverGeoModel.key}: ${e.message}")
                            ifirebaseFailedListener.onFirebaseFailed("Error parsing DriverInfo: ${e.message}")
                        }
                    } else {
                        Log.e("myTAG", "DriverInfo not found for key: ${driverGeoModel.key}")
                        ifirebaseFailedListener.onFirebaseFailed("${getString(R.string.key_not_found)} ${driverGeoModel.key}")
                    }
                }
            })
    }

        override fun onMapReady(googleMap: GoogleMap) {
            mMap = googleMap
            //Enable zoom
            mMap.uiSettings.isZoomControlsEnabled = true
            Dexter.withContext(requireContext())
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : PermissionListener {
                    @SuppressLint("ResourceType")
                    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
                    override fun onPermissionGranted(p0: PermissionGrantedResponse?) {

                        mMap.isMyLocationEnabled = true
                        mMap.uiSettings.isMyLocationButtonEnabled = true
                        Log.d("myTAG", "Quyền truy cập vị trí được cấp - Nút My Location đã được bật")

                        mMap.setOnMyLocationClickListener {
                            Toast.makeText(requireContext(), "Click button!", Toast.LENGTH_SHORT).show()
                            fusedLocationProviderClient.lastLocation
                                .addOnFailureListener { e ->
                                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                                }
                                .addOnSuccessListener { location ->
                                    val userLatLng = LatLng(location.latitude, location.longitude)
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f))
                                }
                            true
                        }

                        // Start location updates after permission is granted
                        fusedLocationProviderClient.requestLocationUpdates(
                            locationRequest,
                            locationCallback,
                            Looper.myLooper()
                        )

                        val locationButton = (mapFragment.requireView()
                            .findViewById<View>(1)?.parent as? View)?.findViewById<View>(2)
                        if (locationButton != null) {
                            val params = locationButton.layoutParams as? RelativeLayout.LayoutParams
                            if (params != null) {
                                params.addRule(RelativeLayout.ALIGN_TOP, 0)
                                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                                val density = resources.displayMetrics.density
                                params.bottomMargin = (50 * density).toInt() // 50dp
                                locationButton.layoutParams = params
                                Log.d("myTAG", "Đã điều chỉnh vị trí nút My Location thành công")
                            } else {
                                Log.e("myTAG", "Không thể điều chỉnh giao diện nút - Layout params không phải RelativeLayout")
                            }
                        } else {
                            Log.e("myTAG", "Không tìm thấy nút My Location - Hãy thử dùng nút tùy chỉnh")
                        }
                    }

                    override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                        Toast.makeText(requireContext(), "Quyền truy cập vị trí bị từ chối: ${p0?.permissionName}", Toast.LENGTH_SHORT).show()
                        Log.e("myTAG", "Quyền truy cập vị trí bị từ chối")
                    }

                    override fun onPermissionRationaleShouldBeShown(p0: PermissionRequest?, p1: PermissionToken?) {
                        p1?.continuePermissionRequest()
                    }

                }).check()

            // Áp dụng phong cách bản đồ
            try {
                val success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.uber_maps_style)
                )
                if (!success) {
                    Log.e("myTAG", "Lỗi tải phong cách bản đồ")
                }
            } catch (e: Resources.NotFoundException) {
                Log.e("myTAG", "Lỗi tải phong cách: ${e.message}", e)
            }
        }


        override fun onPause() {
            super.onPause()
            if (::fusedLocationProviderClient.isInitialized && ::locationCallback.isInitialized) {
                fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            }
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }

        override fun onDriverInfoLoadSuccess(driverGeoModel: DriverGeoModel?) {
            //If already have marker with this key, doesn't set it again
            if (!Common.markerList.containsKey(driverGeoModel?.key))
                Common.markerList.put(driverGeoModel?.key!!, mMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(driverGeoModel.geolocation!!.latitude, driverGeoModel.geolocation!!.longitude))
                        .flat(true)
                        .title(Common.buildName(driverGeoModel.driverInfoModel!!.firstName, driverGeoModel.driverInfoModel!!.lastName))
                        .snippet(driverGeoModel.driverInfoModel!!.phoneNumber)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.car))))

            if (!TextUtils.isEmpty(cityName)) {
                val driverLocation = FirebaseDatabase.getInstance()
                    .getReference(Common.DRIVERS_LOCATION_REFERENCES)
                    .child(cityName)
                    .child(driverGeoModel!!.key!!)
                driverLocation.addValueEventListener(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {
                        Snackbar.make(requireView(), error.message, Snackbar.LENGTH_SHORT).show()
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        if (!p0.hasChildren()) {
                            if (Common.markerList.containsKey(driverGeoModel.key)) {
                                val marker = Common.markerList.get(driverGeoModel!!.key)
                                marker!!.remove() // Remove marker from map
                                Common.markerList.remove(driverGeoModel!!.key) // Remove marker information
                                Common.driversSubscribe.remove(driverGeoModel.key) // Remove driver information
                                driverLocation.removeEventListener(this)
                            }
                        }
                        else{
                            if (Common.markerList.get(driverGeoModel!!.key) != null)
                            {
                                val geoQueryModel = p0!!.getValue(GeoQueryModel::class.java)
                                val animationModel = AnimationMode(isRun = true, geoQueryModel!!)
                                if (Common.driversSubscribe.get(driverGeoModel.key) != null) {
                                    val marker = Common.markerList.get(driverGeoModel!!.key)
                                    val oldPosition = Common.driversSubscribe.get(driverGeoModel.key)!!

                                    val from = StringBuilder()
                                        .append(oldPosition!!.geoQueryModel!!.l?.get(0))
                                        .append(",")
                                        .append(oldPosition!!.geoQueryModel!!.l?.get(1))
                                        .toString()

                                    val to = StringBuilder()
                                        .append(animationModel.geoQueryModel!!.l?.get(0))
                                        .append(",")
                                        .append(animationModel.geoQueryModel!!.l?.get(1))
                                        .toString()

                                    moveMarkerAnimation(driverGeoModel.key!!, animationModel, marker, from.toString(), to.toString())
                                }
                                else
                                    Common.driversSubscribe.put(driverGeoModel.key!!, animationModel) // First location init
                            }
                        }
                    }
                })
            }
        }

        private fun moveMarkerAnimation(
            key: String,
            newData: AnimationMode,
            marker: Marker?,
            from: String,
            to: String
            ) {
            Log.d("myTAG","moveMarkerAnimation() loading")
            // Function body will be here
            if (newData.isRun)  {
                //Request API
                compositeDisposable.add(iGoogleAPI.getDirections(
                        "driving",
                        "less_driving",
                        from,
                        to,
                        getString(R.string.google_api_key)
                    )
                        !!.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { returnResult ->
                            Log.d("myTAG","API_RETURN,$returnResult")
                            try {
                                val jsonObject = JSONObject(returnResult)
                                val jsonArray = jsonObject.getJSONArray("routes")
                                for (i in 0 until jsonArray.length()) {
                                    val route = jsonArray.getJSONObject(i)
                                    val poly = route.getJSONObject("overview_polyline")
                                    val polyline = poly.getString("points")
                                    //polylineList = Common.decodePoly(polyline) as ArrayList<LatLng?>?
                                    newData.polylineList = Common.decodePoly(polyline) as ArrayList<LatLng?>?
                                }
                                //Moving


//                                index = -1
//                                next = 1
                                newData.index = -1
                                newData.next = 1

                                val runnable = object : Runnable {
                                    override fun run() {
                                        if (newData.polylineList != null && newData.polylineList!!.size > 1) {
                                            if (newData.index < newData.polylineList!!.size - 2) {
                                                newData.index++
                                                newData.next = newData.index + 1
                                                newData.start = newData.polylineList!![newData.index]!!
                                                newData.end = newData.polylineList!![newData.next]!!
                                            }
                                            val valueAnimator = ValueAnimator.ofInt(0, 1)
                                            valueAnimator.duration = 3000
                                            valueAnimator.interpolator = LinearInterpolator()
                                            valueAnimator.addUpdateListener { animation ->
                                                val v = animation.animatedFraction
                                                newData.lat = v * newData.end!!.latitude + (1 - v) * newData.start!!.latitude
                                                newData.lng = v * newData.end!!.longitude + (1 - v) * newData.start!!.longitude
                                                val newPos = LatLng(newData.lat, newData.lng)
                                                marker!!.position = newPos
                                                marker!!.setAnchor(0.5f, 0.5f)
                                                marker!!.rotation = Common.getBearing(newData.start!!, newPos)
                                            }
                                            valueAnimator.start()

                                            if (newData.index < newData.polylineList!!.size - 2) {
                                                newData.handler!!.postDelayed(this, 1500)
                                            } else if (newData.index < newData.polylineList!!.size - 1) {
                                                newData.isRun = false
                                                Common.driversSubscribe.put(key, newData) // Update
                                            }
                                        }
                                    }
                                }

                                newData.handler!!.postDelayed(runnable, 1500)

                            } catch (e: java.lang.Exception)
                            {
                    Snackbar.make(requireView(), e.message.toString(), Snackbar.LENGTH_LONG).show()
                }
                        }
                )
            }
        }


        override fun onFirebaseFailed(message: String) {
            TODO("Not yet implemented")
        }

}



