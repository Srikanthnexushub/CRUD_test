import React, { useState, useEffect } from 'react';
import { User } from '../types';
import './UserList.css';

interface UserListProps {
  refresh?: number;
}

const UserList: React.FC<UserListProps> = ({ refresh }) => {
  const [users, setUsers] = useState<User[]>([]);

  useEffect(() => {
    // For now, show placeholder since we don't have a GET users endpoint yet
    setUsers([
      {
        id: 0,
        username: 'Demo User',
        email: 'demo@example.com',
        role: 'ROLE_USER',
        createdAt: new Date().toISOString(),
        isDemo: true
      }
    ]);
  }, [refresh]);

  if (users.length === 0) {
    return (
      <div className="user-list empty">
        <p>No users registered yet. Register your first user above!</p>
      </div>
    );
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
              <td>{user.createdAt ? new Date(user.createdAt).toLocaleString() : 'N/A'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default UserList;
