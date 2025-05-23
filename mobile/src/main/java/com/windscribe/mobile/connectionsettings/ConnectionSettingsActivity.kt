/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.connectionsettings

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import butterknife.BindView
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.mobile.base.BaseActivity
import com.windscribe.mobile.custom_view.preferences.*
import com.windscribe.mobile.di.ActivityModule
import com.windscribe.mobile.dialogs.*
import com.windscribe.mobile.gpsspoofing.GpsSpoofingSettingsActivity
import com.windscribe.mobile.networksecurity.NetworkSecurityActivity
import com.windscribe.mobile.splittunneling.SplitTunnelingActivity
import com.windscribe.mobile.utils.PermissionManager
import com.windscribe.mobile.utils.UiUtil
import com.windscribe.vpn.constants.FeatureExplainer
import org.slf4j.LoggerFactory
import java.util.zip.GZIPOutputStream
import javax.inject.Inject

class ConnectionSettingsActivity : BaseActivity(), ConnectionSettingsView, ExtraDataUseWarningDialogCallBack {

    private val logger = LoggerFactory.getLogger(TAG)

    @Inject
    lateinit var presenter: ConnectionSettingsPresenter

    @BindView(R.id.nav_title)
    lateinit var activityTitleView: TextView

    @BindView(R.id.connection_parent)
    lateinit var constraintLayoutConnection: ConstraintLayout

    @BindView(R.id.split_tunnel_status)
    lateinit var splitTunnelStatusView: TextView

    @BindView(R.id.cl_boot_settings)
    lateinit var autoStartToggleView: ToggleView

    @BindView(R.id.cl_lan_settings)
    lateinit var allowLanToggleView: ToggleView

    @BindView(R.id.cl_gps_settings)
    lateinit var gpsToggleView: ToggleView

    @BindView(R.id.cl_connection_mode)
    lateinit var connectionModeDropDownView: ExpandableDropDownView

    @BindView(R.id.cl_dns_mode)
    lateinit var dnsModeDropDownView: ExpandableDropDownView

    @BindView(R.id.cl_packet_size)
    lateinit var packetSizeModeDropDownView: ExpandableDropDownView

    @BindView(R.id.cl_keep_alive)
    lateinit var keepAliveExpandableView: ExpandableDropDownView

    @BindView(R.id.cl_decoy_traffic)
    lateinit var decoyTrafficToggleView: ExpandableToggleView

    @BindView(R.id.cl_anti_censorship)
    lateinit var clAntiCensorshipToggleView: ToggleView

    @BindView(R.id.cl_auto_connect)
    lateinit var clAutoConnectToggleView: ToggleView

    @BindView(R.id.split_tunnel_title)
    lateinit var splitTunnelLabel: TextView

    @BindView(R.id.split_tunnel_right_icon)
    lateinit var splitTunnelArrow: ImageView

    @BindView(R.id.network_options_title)
    lateinit var networkOptionsLabel: TextView

    @BindView(R.id.network_options_right_icon)
    lateinit var networkOptionsArrow: ImageView

    @Inject
    lateinit var permissionManager: PermissionManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.connection_layout, true)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        setupCustomViewDelegates()
        logger.info("Setting up layout based on saved mode settings...")
        presenter.init()
        activityTitleView.text = getString(R.string.connection)
        permissionManager.register(this)
    }

    override fun onStart() {
        super.onStart()
        presenter.onStart()
    }

    override fun onResume() {
        super.onResume()
        presenter.onHotStart()
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    override fun gotoSplitTunnelingSettings() {
        val intent = SplitTunnelingActivity.getStartIntent(this)
        val options = ActivityOptions.makeSceneTransitionAnimation(this)
        startActivity(intent, options.toBundle())
    }

    private fun setupCustomViewDelegates() {
        allowLanToggleView.delegate = object : ToggleView.Delegate {
            override fun onToggleClick() {
                logger.info("User clicked on allow lan...")
                presenter.onAllowLanClicked()
            }

            override fun onExplainClick() {
                openURLInBrowser(FeatureExplainer.ALLOW_LAN)
            }
        }
        gpsToggleView.delegate = object : ToggleView.Delegate {
            override fun onToggleClick() {
                logger.info("User clicked on gps spoof.")
                presenter.onGpsSpoofingClick()
            }

            override fun onExplainClick() {
                openURLInBrowser(FeatureExplainer.GPS_SPOOFING)
            }
        }
        autoStartToggleView.delegate = object : ToggleView.Delegate {
            override fun onToggleClick() {
                logger.info("User clicked on auto start on boot.")
                presenter.onAutoStartOnBootClick()
            }

            override fun onExplainClick() {}
        }
        decoyTrafficToggleView.delegate = object : ExpandableToggleView.Delegate {
            override fun onToggleClick() {
                presenter.onDecoyTrafficClick()
            }

            override fun onExplainClick() {
                openURLInBrowser(FeatureExplainer.DECOY_TRAFFIC)
            }
        }
        val decoyTrafficView = decoyTrafficToggleView.childView as DecoyTrafficView?
        decoyTrafficView?.delegate = object : DecoyTrafficView.Delegate {
            override fun onDecoyTrafficVolumeChanged(volume: String) {
                presenter.onFakeTrafficVolumeSelected(volume)
            }
        }
        connectionModeDropDownView.delegate = object : ExpandableDropDownView.Delegate {
            override fun onItemSelect(position: Int) {
                if (position == 0) {
                    presenter.onConnectionModeAutoClicked()
                } else {
                    presenter.onConnectionModeManualClicked()
                }
            }

            override fun onExplainClick() {
                openURLInBrowser(FeatureExplainer.CONNECTION_MODE)
            }
        }
        dnsModeDropDownView.delegate = object : ExpandableDropDownView.Delegate {
            override fun onItemSelect(position: Int) {
                if (position == 0) {
                    presenter.onRobertDnsModeSelected()
                } else {
                    presenter.onCustomDnsModeSelected()
                }
            }

            override fun onExplainClick() {
                openURLInBrowser(FeatureExplainer.CUSTOM_DNS_MODE)
            }
        }
        val connectionModeView = connectionModeDropDownView.childView as ConnectionModeView?
        connectionModeView?.delegate = object : ConnectionModeView.Delegate {
            override fun onProtocolSelected(protocol: String) {
                presenter.onProtocolSelected(protocol)
            }

            override fun onPortSelected(protocol: String, port: String) {
                presenter.onPortSelected(protocol, port)
            }
        }
        val dnsModeView = dnsModeDropDownView.childView as DnsModeView?
        dnsModeView?.delegate = object : DnsModeView.Delegate {
            override fun onCustomDnsChanged(dns: String) {
                presenter.onCustomDnsChanged(dns)
            }
        }
        packetSizeModeDropDownView.delegate = object : ExpandableDropDownView.Delegate {
            override fun onItemSelect(position: Int) {
                if (position == 0) {
                    presenter.onPacketSizeAutoModeClicked()
                } else {
                    presenter.onPacketSizeManualModeClicked()
                }
            }

            override fun onExplainClick() {
                openURLInBrowser(FeatureExplainer.PACKET_SIZE)
            }
        }
        val packetSizeView = packetSizeModeDropDownView.childView as PacketSizeView
        packetSizeView.delegate = object : PacketSizeView.Delegate {
            override fun onAutoFillButtonClick() {
                presenter.onAutoFillPacketSizeClicked()
            }

            override fun onPacketSizeChanged(packetSize: String) {
                presenter.setPacketSize(packetSize)
            }
        }
        keepAliveExpandableView.delegate = object : ExpandableDropDownView.Delegate {
            override fun onItemSelect(position: Int) {
                if (position == 0) {
                    presenter.onKeepAliveAutoModeClicked()
                } else {
                    presenter.onKeepAliveManualModeClicked()
                }
            }

            override fun onExplainClick() {}
        }
        val keepAliveView = keepAliveExpandableView.childView as KeepAliveView
        keepAliveView.delegate = object : KeepAliveView.Delegate {
            override fun onKeepAliveTimeChanged(time: String) {
                presenter.saveKeepAlive(time)
            }

        }
        splitTunnelArrow.tag = R.drawable.ic_forward_arrow_settings
        networkOptionsArrow.tag = R.drawable.ic_forward_arrow_settings
        UiUtil.setupOnTouchListener(
            textViewContainer = splitTunnelLabel,
            iconView = splitTunnelArrow,
            textView = splitTunnelLabel
        )
        UiUtil.setupOnTouchListener(
            textViewContainer = networkOptionsLabel,
            iconView = networkOptionsArrow,
            textView = networkOptionsLabel
        )
        clAntiCensorshipToggleView.delegate = object : ToggleView.Delegate {
            override fun onToggleClick() {
                presenter.onAntiCensorshipClick()
            }
            override fun onExplainClick() {
                openURLInBrowser(FeatureExplainer.CIRCUMVENT_CENSORSHIP)
            }
        }
        clAutoConnectToggleView.delegate = object : ToggleView.Delegate {
            override fun onToggleClick() {
                presenter.onAutoConnectClick()
            }
            override fun onExplainClick() {}
        }
    }

    @OnClick(R.id.nav_button)
    fun onBackButtonClicked() {
        logger.info("User clicked on back arrow...")
        onBackPressed()
    }

    @OnClick(R.id.split_tunnel_title, R.id.split_tunnel_status, R.id.split_tunnel_right_icon)
    fun onSplitTunnelingClick() {
        logger.info("User clicked on split tunneling...")
        presenter.onSplitTunnelingOptionClicked()
    }

    @OnClick(R.id.open_always_setting)
    fun openAlwaysVPNSettingsClick() {
        logger.info("User clicked on open VPN Settings...")
        goToSettings()
    }

    private fun goToSettings() {
        val intent = Intent("android.net.vpn.SETTINGS")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    @OnClick(R.id.network_options_right_icon, R.id.network_options_title)
    fun onWhitelistClick() {
        presenter.onNetworkOptionsClick()
    }

    override fun openGpsSpoofSettings() {
        startActivity(GpsSpoofingSettingsActivity.getStartIntent(this))
    }

    override fun packetSizeDetectionProgress(progress: Boolean) {
        (packetSizeModeDropDownView.childView as PacketSizeView).packetSizeDetectionProgress(
            progress
        )
    }

    override fun setAutoStartOnBootToggle(toggleDrawable: Int) {
        autoStartToggleView.setToggleImage(toggleDrawable)
    }

    override fun setGpsSpoofingToggle(toggleDrawable: Int) {
        gpsToggleView.setToggleImage(toggleDrawable)
    }

    override fun setKeepAlive(keepAlive: String) {
        (keepAliveExpandableView.childView as KeepAliveView).setKeepAlive(keepAlive)
    }

    override fun setLanBypassToggle(toggleDrawable: Int) {
        allowLanToggleView.setToggleImage(toggleDrawable)
    }

    override fun setPacketSize(size: String) {
        val packetSizeView = packetSizeModeDropDownView.childView as PacketSizeView
        packetSizeView.setPacketSize(size)
    }

    override fun setSplitTunnelText(onOff: String, color: Int) {
        logger.info("Setting tunnel status $onOff")
        splitTunnelStatusView.text = onOff
        splitTunnelStatusView.setTextColor(color)
    }

    override fun setupPacketSizeModeAdapter(savedValue: String, types: Array<String>) {
        packetSizeModeDropDownView.setAdapter(savedValue, types)
    }

    override fun setKeepAliveModeAdapter(savedValue: String, types: Array<String>) {
        keepAliveExpandableView.setAdapter(savedValue, types)
    }

    override fun setupPortMapAdapter(port: String, portMap: List<String>) {
        val connectionModeView = connectionModeDropDownView.childView as ConnectionModeView
        connectionModeView.sePortAdapter(port, portMap)
    }

    override fun setupProtocolAdapter(protocol: String, protocols: Array<String>) {
        val connectionModeView = connectionModeDropDownView.childView as ConnectionModeView
        connectionModeView.seProtocolAdapter(protocol, protocols)
    }

    override fun showGpsSpoofing() {
        gpsToggleView.visibility = View.VISIBLE
    }

    override fun showToast(toastString: String) {
        runOnUiThread {
            Toast.makeText(this, toastString, Toast.LENGTH_SHORT).show()
        }
    }

    override fun showExtraDataUseWarning() {
        ExtraDataUseWarningDialog.show(this)
    }

    override fun turnOnDecoyTraffic() {
        presenter.turnOnDecoyTraffic()
    }

    override fun showAutoStartOnBoot() {
        autoStartToggleView.visibility = View.VISIBLE
    }

    override fun setKeepAliveContainerVisibility(isAutoKeepAlive: Boolean) {
        keepAliveExpandableView.visibility =
            if (isAutoKeepAlive) View.VISIBLE else View.GONE
    }

    override fun setupConnectionModeAdapter(savedValue: String, connectionModes: Array<String>) {
        connectionModeDropDownView.setAdapter(savedValue, connectionModes)
    }
    override fun setupDNSModeAdapter(savedValue: String, dnsModes: Array<String>) {
        dnsModeDropDownView.setAdapter(savedValue, dnsModes)
    }

    override fun setupFakeTrafficVolumeAdapter(selectedValue: String, values: Array<String>) {
        (decoyTrafficToggleView.childView as DecoyTrafficView).setAdapter(
            selectedValue,
            values
        )
    }

    override fun setPotentialTrafficUse(value: String) {
        (decoyTrafficToggleView.childView as DecoyTrafficView).setPotentialTraffic(value)
    }

    override fun setDecoyTrafficToggle(toggleDrawable: Int) {
        decoyTrafficToggleView.setToggleImage(toggleDrawable)
    }

    override fun setAntiCensorshipToggle(toggleDrawable: Int) {
        clAntiCensorshipToggleView.setToggleImage(toggleDrawable)
    }

    override fun setAutoConnectToggle(toggleDrawable: Int) {
        clAutoConnectToggleView.setToggleImage(toggleDrawable)
    }

    override fun goToNetworkSecurity() {
        startActivity(NetworkSecurityActivity.getStartIntent(this))
    }

    override fun setCustomDnsAddress(dnsAddress: String) {
        val dnsModeView = dnsModeDropDownView.childView as DnsModeView
        dnsModeView.setCustomDns(dnsAddress)
    }

    companion object {
        private const val TAG = "conn_settings_a"
        fun getStartIntent(context: Context?): Intent {
            return Intent(context, ConnectionSettingsActivity::class.java)
        }
    }
}