import { useState, useEffect } from 'react'
import './UserList.css'

const UserList = ({ refresh }) => {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    // For now, show placeholder since we don't have a GET users endpoint yet
    setUsers([
      {
        id: 'demo',
        username: 'Demo User',
        email: 'demo@example.com',
        createdAt: new Date().toISOString(),
        isDemo: true
      }
    ])
  }, [refresh])

  if (loading) {
    return <div className="user-list loading">Loading users...</div>
  }

  if (users.length === 0) {
    return (
      <div className="user-list empty">
        <p>No users registered yet. Register your first user above!</p>
      </div>
    )
  }

  return (
    <div className="user-list">
      <div className="info-message">
        ℹ️ Note: GET /api/users endpoint not implemented yet. This shows demo data.
        <br />
        Check PostgreSQL database to see actual registered users.
      </div>
      <table className="users-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Username</th>
            <th>Email</th>
            <th>Registered At</th>
          </tr>
        </thead>
        <tbody>
          {users.map((user) => (
            <tr key={user.id} className={user.isDemo ? 'demo-row' : ''}>
              <td>{user.id}</td>
              <td>{user.username}</td>
              <td>{user.email}</td>
              <td>{new Date(user.createdAt).toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default UserList
