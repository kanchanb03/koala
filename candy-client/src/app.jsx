import React, { useState, useEffect, useCallback } from 'react'

const api = {
  prefix: process.env.NODE_ENV === 'production' ? '/api' : '',
  async get(path) {
    const res = await fetch(this.prefix + path)
    if (!res.ok) throw new Error(await res.text() || res.status)
    return res.json()
  },
  async post(path, body) {
    const res = await fetch(this.prefix + path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
    if (!res.ok) throw new Error(await res.text() || res.status)
    return res.json()
  },
  async put(path, body) {
    const res = await fetch(this.prefix + path, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
    if (!res.ok) throw new Error(await res.text() || res.status)
    return res.json()
  },
  async delete(path) {
    const res = await fetch(this.prefix + path, { method: 'DELETE' })
    if (!res.ok) throw new Error(await res.text() || res.status)
  },
}

export default function App() {
  const [inventory, setInventory] = useState([])
  const [queryTitle, setQueryTitle] = useState('')
  const [queryRows, setQueryRows] = useState([])
  const [newName, setNewName] = useState('')
  const [updateForm, setUpdateForm] = useState({ id: '', stock: '', cap: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const fetchInventory = useCallback(async () => {
    setLoading(true)
    try {
      const data = await api.get('/inventory')
      setInventory(data.sort((a, b) => a.id - b.id))
      setError('')
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchInventory()
  }, [fetchInventory])

  // Live updates
  useEffect(() => {
    const es = new EventSource(api.prefix + '/stream/inventory')
    es.onmessage = ({ data }) => {
      try {
        const updated = JSON.parse(data)
        setInventory(updated.sort((a, b) => a.id - b.id))
      } catch (e) {
        console.warn('Bad SSE payload', e)
      }
    }
    return () => es.close()
  }, [])

  const mutate = async (fn) => {
    try {
      await fn()
      setError('')
    } catch (e) {
      setError(e.message)
    } finally {
      fetchInventory()
    }
  }

  const ensureItem = async (name) => {
    try {
      const { id } = await api.post('/items', { name })
      return id
    } catch {
      const items = await api.get('/items')
      return items.find((i) => i.name.toLowerCase() === name.toLowerCase())?.id
    }
  }

  const addCandy = () => mutate(async () => {
    const name = newName.trim()
    if (!name) return
    const id = await ensureItem(name)
    if (!id) throw new Error('Could not create item')
    await api.post('/inventory', { item: id, stock: 0, capacity: 100 })
    setNewName('')
  })

  const updateInventory = () => {
    const { id, stock, cap } = updateForm
    if (!id || !stock || !cap) return
    mutate(() => api.put(`/inventory/${id}`, {
      stock: Number(stock),
      capacity: Number(cap),
    }))
    setUpdateForm({ id: '', stock: '', cap: '' })
  }

  const deleteRow = (row) =>
    mutate(() => api.delete(`/inventory/${row.id}`))

  const runQuery = (title, path) => mutate(async () => {
    const rows = await api.get(path)
    setQueryTitle(title)
    setQueryRows(rows)
  })

  return (
    <div style={{ padding: 16, fontFamily: 'system-ui' }}>
      <h1 style={{ fontSize: 24, marginBottom: 12 }}>Candy Inventory 🍬</h1>

      {error && <div style={{ color: 'red' }}>{error}</div>}
      {loading && <div>Loading...</div>}

      <div style={{ display: 'flex', gap: 16 }}>
        {/* Inventory Table */}
        <section style={{ flex: 3, minWidth: 700 }}>
          <h2>Current Inventory</h2>
          <a href={api.prefix + '/export?table=inventory'} download>
            Download CSV
          </a>
          <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: 8 }}>
            <thead>
              <tr>
                {['Inv ID', 'Item', 'Stock', 'Cap.', ''].map((h, i) => (
                  <th
                    key={i}
                    style={{
                      padding: '4px',
                      borderBottom: '1px solid #ddd',
                      textAlign: i === 0 ? 'left' : 'center',
                      fontWeight: 600,
                    }}
                  >
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {inventory.map((r) => (
                <tr key={r.id}>
                  <td style={{ padding: '4px' }}>{r.id}</td>
                  <td style={{ padding: '4px' }}>{r.item_name}</td>
                  <td style={{ padding: '4px', textAlign: 'center' }}>{r.amount_in_stock}</td>
                  <td style={{ padding: '4px', textAlign: 'center' }}>{r.total_capacity}</td>
                  <td style={{ padding: '4px', textAlign: 'center' }}>
                    <button onClick={() => deleteRow(r)}>Del</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        {/* Sidebar */}
        <aside style={{ flex: 1, minWidth: 320 }}>
          <h2>Quick Queries</h2>
          {['Out of Stock','Low Stock','Overstocked'].map((t) => (
            <button
              key={t}
              style={{ marginRight: 8 }}
              onClick={() => runQuery(t, `/inventory/${t.toLowerCase().replace(/ /g,'-')}`)}
            >
              {t.split(' ')[0]}
            </button>
          ))}

          {queryRows.length > 0 && (
            <>
              <h3 style={{ marginTop: 12 }}>{queryTitle}</h3>
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr>{['ID','Item','S','C'].map((h,i) => (
                    <th key={i} style={{ padding: '4px', borderBottom: '1px solid #ddd' }}>{h}</th>
                  ))}</tr>
                </thead>
                <tbody>
                  {queryRows.map((r) => (
                    <tr key={r.id}>
                      <td style={{ padding: '4px' }}>{r.id}</td>
                      <td style={{ padding: '4px' }}>{r.item_name}</td>
                      <td style={{ padding: '4px', textAlign: 'center' }}>{r.amount_in_stock}</td>
                      <td style={{ padding: '4px', textAlign: 'center' }}>{r.total_capacity}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </>
          )}

          <h3 style={{ marginTop: 16 }}>Add Candy</h3>
          <div style={{ display: 'flex', gap: 8 }}>
            <input
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              placeholder="Name"
            />
            <button onClick={addCandy}>Add</button>
          </div>

          <h3 style={{ marginTop: 16 }}>Update Inventory</h3>
          <div style={{ display: 'flex', gap: 8 }}>
            {['id','stock','cap'].map((f) => (
              <input
                key={f}
                style={{ width: 60 }}
                placeholder={f.toUpperCase()}
                value={updateForm[f]}
                onChange={(e) => setUpdateForm({ ...updateForm, [f]: e.target.value })}
              />
            ))}
            <button onClick={updateInventory}>Save</button>
          </div>
        </aside>
      </div>
    </div>
  )
}
