import QRCode from 'qrcode'
import { useEffect, useState } from 'react'
import {
  cancelPairingSession,
  getLocalDeviceIdentity,
  getPairedDevices,
  getPairingSessionStatus,
  removePairedDevice,
  startPairingSession,
  updateLocalDeviceName,
  type DeviceIdentity,
  type PairedDevice,
  type PairingQrPayload
} from '../../api/desktopApi'

export function DeviceSyncPage() {
  const [identity, setIdentity] = useState<DeviceIdentity | null>(null)
  const [deviceName, setDeviceName] = useState('')
  const [devices, setDevices] = useState<PairedDevice[]>([])
  const [payload, setPayload] = useState<PairingQrPayload | null>(null)
  const [qrDataUrl, setQrDataUrl] = useState('')
  const [status, setStatus] = useState('Ready')
  const [busy, setBusy] = useState(false)

  async function refresh() {
    const [nextIdentity, nextDevices] = await Promise.all([getLocalDeviceIdentity(), getPairedDevices()])
    setIdentity(nextIdentity)
    setDeviceName(nextIdentity.deviceName)
    setDevices(nextDevices)
  }

  useEffect(() => {
    void refresh().catch((error) => setStatus(String(error)))
  }, [])

  useEffect(() => {
    if (!payload) return
    const encoded = JSON.stringify(payload)
    void QRCode.toDataURL(encoded, { margin: 1, width: 280 }).then(setQrDataUrl).catch((error) => setStatus(String(error)))
  }, [payload])

  useEffect(() => {
    if (!payload) return
    const timer = window.setInterval(() => {
      void getPairingSessionStatus(payload.sessionId)
        .then((nextStatus) => {
          setStatus(`Pairing session: ${nextStatus.status}`)
          setDevices(nextStatus.pairedDevices)
          if (nextStatus.status !== 'waiting') {
            setPayload(null)
            setQrDataUrl('')
          }
        })
        .catch((error) => setStatus(String(error)))
    }, 1800)
    return () => window.clearInterval(timer)
  }, [payload])

  async function saveName() {
    if (!deviceName.trim()) {
      setStatus('Device name cannot be empty.')
      return
    }
    setBusy(true)
    try {
      const next = await updateLocalDeviceName(deviceName)
      setIdentity(next)
      setStatus('Local device name saved.')
    } catch (error) {
      setStatus(String(error))
    } finally {
      setBusy(false)
    }
  }

  async function addAndroidDevice() {
    setBusy(true)
    try {
      const nextPayload = await startPairingSession()
      setPayload(nextPayload)
      setStatus('Waiting for Android pairing request. The token expires in 5 minutes.')
    } catch (error) {
      setStatus(String(error))
    } finally {
      setBusy(false)
    }
  }

  async function cancelPairing() {
    if (!payload) return
    setBusy(true)
    try {
      await cancelPairingSession(payload.sessionId)
      setPayload(null)
      setQrDataUrl('')
      setStatus('Pairing cancelled.')
    } catch (error) {
      setStatus(String(error))
    } finally {
      setBusy(false)
    }
  }

  async function unpair(deviceId: string) {
    setBusy(true)
    try {
      await removePairedDevice(deviceId)
      await refresh()
      setStatus('Device unpaired locally.')
    } catch (error) {
      setStatus(String(error))
    } finally {
      setBusy(false)
    }
  }

  const payloadText = payload ? JSON.stringify(payload) : ''
  const expiresIn = payload ? Math.max(0, Math.ceil((payload.expiresAt - Date.now()) / 1000)) : 0

  return (
    <div className="stack">
      <section className="hero">
        <p className="eyebrow">Devices & Sync</p>
        <h1>Pair your own devices, without syncing data yet.</h1>
        <p>Phase 3 only creates a trusted device relationship. SummaryEvent sync is intentionally not enabled here.</p>
      </section>

      <section className="card">
        <h3>Local desktop identity</h3>
        {identity ? (
          <div className="form">
            <label>
              Device name
              <input value={deviceName} onChange={(event) => setDeviceName(event.target.value)} />
            </label>
            <p>
              Type: <strong>{identity.deviceType}</strong>
            </p>
            <p className="muted">Device ID: {identity.deviceId}</p>
            <div className="actions">
              <button className="primary" disabled={busy} onClick={() => void saveName()}>
                Save name
              </button>
              <button disabled={busy} onClick={() => void refresh()}>
                Refresh
              </button>
            </div>
          </div>
        ) : (
          <p>Loading local identity...</p>
        )}
      </section>

      <section className="card">
        <h3>Add Android device</h3>
        <p>Open the Android app, go to Devices & Sync, then scan this QR code or paste the payload for emulator testing.</p>
        <div className="actions">
          <button className="primary" disabled={busy} onClick={() => void addAndroidDevice()}>
            Add Android device
          </button>
          {payload && (
            <button disabled={busy} onClick={() => void cancelPairing()}>
              Cancel pairing
            </button>
          )}
        </div>
        {payload && (
          <div className="pairing-panel">
            {qrDataUrl && <img className="qr-code" src={qrDataUrl} alt="Pairing QR code" />}
            <div>
              <p>
                LAN address: <strong>{payload.host}:{payload.port}</strong>
              </p>
              <p>Expires in about {expiresIn} seconds.</p>
              <p className="muted">
                If Android cannot connect, allow TCP port {payload.port} in Windows Firewall or use a private Wi-Fi/hotspot.
                Public Wi-Fi may block device-to-device pairing.
              </p>
              <textarea readOnly value={payloadText} aria-label="Pairing QR payload" />
            </div>
          </div>
        )}
      </section>

      <section className="card">
        <h3>Connection diagnostics</h3>
        <p>
          Phase 3 uses local LAN pairing only. The desktop listens on a temporary pairing service, preferably fixed port
          <strong> 58231</strong>. If pairing fails, check these first:
        </p>
        <ul className="diagnostic-list">
          <li>Windows network profile should ideally be Private, not Public.</li>
          <li>Windows Firewall must allow inbound TCP on the displayed pairing port.</li>
          <li>Some school, company, hotel, and mall Wi-Fi networks block devices from reaching each other.</li>
          <li>For development with USB, use adb reverse and change the payload host to 127.0.0.1.</li>
        </ul>
      </section>

      <section className="card">
        <h3>Paired devices</h3>
        {devices.length === 0 ? (
          <p>No paired devices yet.</p>
        ) : (
          <div className="device-list">
            {devices.map((device) => (
              <article key={device.pairedDeviceId} className="device-row">
                <div>
                  <strong>{device.pairedDeviceName}</strong>
                  <p>
                    {device.pairedDeviceType} · {device.pairingStatus} · trusted
                  </p>
                  <small>Paired at {new Date(device.pairedAt).toLocaleString()}</small>
                </div>
                <button disabled={busy} onClick={() => void unpair(device.pairedDeviceId)}>
                  Unpair
                </button>
              </article>
            ))}
          </div>
        )}
      </section>

      <section className="card">
        <h3>Status</h3>
        <p>{status}</p>
      </section>
    </div>
  )
}
