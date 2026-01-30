import React, { useState, useEffect } from 'react';
import api from '../../services/api';
import '../../styles/Notifications.css';

const EmailTemplateEditor = () => {
    const [templates, setTemplates] = useState([]);
    const [selectedTemplate, setSelectedTemplate] = useState(null);
    const [templateContent, setTemplateContent] = useState('');
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);
    const [saveStatus, setSaveStatus] = useState(null);
    const [showPreview, setShowPreview] = useState(true);

    const templateVariables = {
        'USER_CREATED': [
            { var: '${username}', desc: 'Username of the new user' },
            { var: '${email}', desc: 'Email address of the new user' },
            { var: '${role}', desc: 'Role assigned to the user' },
            { var: '${createdAt}', desc: 'Account creation timestamp' },
        ],
        'USER_UPDATED': [
            { var: '${username}', desc: 'Username of the updated user' },
            { var: '${email}', desc: 'Email address of the user' },
            { var: '${role}', desc: 'Current role of the user' },
            { var: '${updatedBy}', desc: 'Who made the update' },
            { var: '${changes}', desc: 'Summary of changes made' },
        ],
        'USER_DELETED': [
            { var: '${username}', desc: 'Username of the deleted user' },
            { var: '${email}', desc: 'Email address of the deleted user' },
            { var: '${deletedBy}', desc: 'Who deleted the user' },
            { var: '${deletedAt}', desc: 'Deletion timestamp' },
        ],
        'SECURITY_ALERT': [
            { var: '${eventType}', desc: 'Type of security event' },
            { var: '${username}', desc: 'Username involved in the event' },
            { var: '${ipAddress}', desc: 'IP address of the event' },
            { var: '${timestamp}', desc: 'When the event occurred' },
            { var: '${details}', desc: 'Additional event details' },
        ],
        'DAILY_DIGEST': [
            { var: '${date}', desc: 'Date of the digest' },
            { var: '${totalUsers}', desc: 'Total users in the system' },
            { var: '${newUsers}', desc: 'Number of new users today' },
            { var: '${updatedUsers}', desc: 'Number of updated users today' },
            { var: '${deletedUsers}', desc: 'Number of deleted users today' },
            { var: '${securityEvents}', desc: 'Number of security events today' },
            { var: '${activities}', desc: 'List of all activities' },
        ],
    };

    useEffect(() => {
        loadTemplates();
    }, []);

    const loadTemplates = async () => {
        try {
            setLoading(true);
            setError(null);
            const response = await api.getEmailTemplates();
            setTemplates(response.data || []);

            if (response.data && response.data.length > 0 && !selectedTemplate) {
                selectTemplate(response.data[0]);
            }
        } catch (err) {
            console.error('Failed to load templates:', err);
            setError(err.message || 'Failed to load email templates');
        } finally {
            setLoading(false);
        }
    };

    const selectTemplate = (template) => {
        setSelectedTemplate(template);
        setTemplateContent(template.htmlContent);
        setSaveStatus(null);
    };

    const handleSave = async () => {
        if (!selectedTemplate) return;

        setSaving(true);
        setSaveStatus(null);

        try {
            await api.updateEmailTemplate(selectedTemplate.id, {
                htmlContent: templateContent,
            });

            setSaveStatus({ type: 'success', message: 'Template saved successfully!' });
            await loadTemplates();
        } catch (err) {
            console.error('Failed to save template:', err);
            setSaveStatus({ type: 'error', message: err.message || 'Failed to save template' });
        } finally {
            setSaving(false);
            setTimeout(() => setSaveStatus(null), 3000);
        }
    };

    const handleReset = () => {
        if (selectedTemplate && window.confirm('Are you sure you want to reset to the saved version?')) {
            setTemplateContent(selectedTemplate.htmlContent);
        }
    };

    const insertVariable = (variable) => {
        const textarea = document.getElementById('template-editor');
        const start = textarea.selectionStart;
        const end = textarea.selectionEnd;
        const text = templateContent;
        const before = text.substring(0, start);
        const after = text.substring(end, text.length);

        setTemplateContent(before + variable + after);

        // Set cursor position after inserted variable
        setTimeout(() => {
            textarea.selectionStart = textarea.selectionEnd = start + variable.length;
            textarea.focus();
        }, 0);
    };

    const hasChanges = selectedTemplate && templateContent !== selectedTemplate.htmlContent;

    return (
        <div className="email-template-editor">
            <div className="editor-header">
                <h2>Email Template Editor</h2>
                <div className="editor-actions">
                    <label className="toggle-preview">
                        <input
                            type="checkbox"
                            checked={showPreview}
                            onChange={(e) => setShowPreview(e.target.checked)}
                        />
                        <span>Show Preview</span>
                    </label>
                </div>
            </div>

            {error && (
                <div className="alert alert-error">
                    {error}
                </div>
            )}

            {saveStatus && (
                <div className={`alert alert-${saveStatus.type}`}>
                    {saveStatus.message}
                </div>
            )}

            <div className="editor-container">
                <div className="template-selector">
                    <h3>Templates</h3>
                    {loading ? (
                        <div className="loading-spinner">Loading...</div>
                    ) : (
                        <div className="template-list">
                            {templates.map((template) => (
                                <div
                                    key={template.id}
                                    className={`template-item ${selectedTemplate?.id === template.id ? 'active' : ''}`}
                                    onClick={() => selectTemplate(template)}
                                >
                                    <div className="template-name">{template.name}</div>
                                    <div className="template-type">{template.type}</div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                <div className="editor-main">
                    {selectedTemplate ? (
                        <>
                            <div className="template-info">
                                <h3>{selectedTemplate.name}</h3>
                                <span className="template-badge">{selectedTemplate.type}</span>
                            </div>

                            <div className="editor-content">
                                <div className="code-editor">
                                    <div className="editor-toolbar">
                                        <span>HTML Editor</span>
                                        {hasChanges && (
                                            <button
                                                className="btn-reset"
                                                onClick={handleReset}
                                                disabled={saving}
                                            >
                                                Reset
                                            </button>
                                        )}
                                    </div>
                                    <textarea
                                        id="template-editor"
                                        value={templateContent}
                                        onChange={(e) => setTemplateContent(e.target.value)}
                                        className="html-textarea"
                                        spellCheck="false"
                                        disabled={saving}
                                    />
                                </div>

                                {showPreview && (
                                    <div className="template-preview">
                                        <div className="preview-toolbar">
                                            <span>Preview</span>
                                        </div>
                                        <iframe
                                            srcDoc={templateContent}
                                            title="Template Preview"
                                            className="preview-iframe"
                                        />
                                    </div>
                                )}
                            </div>

                            <div className="editor-footer">
                                <button
                                    className="btn-save"
                                    onClick={handleSave}
                                    disabled={!hasChanges || saving}
                                >
                                    {saving ? 'Saving...' : 'Save Template'}
                                </button>
                            </div>
                        </>
                    ) : (
                        <div className="empty-state">
                            <i className="fas fa-file-code"></i>
                            <p>Select a template to edit</p>
                        </div>
                    )}
                </div>

                <div className="variables-sidebar">
                    <h3>Available Variables</h3>
                    {selectedTemplate && templateVariables[selectedTemplate.type] ? (
                        <div className="variables-list">
                            <p className="variables-info">
                                Click a variable to insert it at the cursor position
                            </p>
                            {templateVariables[selectedTemplate.type].map((variable, index) => (
                                <div
                                    key={index}
                                    className="variable-item"
                                    onClick={() => insertVariable(variable.var)}
                                >
                                    <code className="variable-code">{variable.var}</code>
                                    <span className="variable-desc">{variable.desc}</span>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="variables-empty">
                            <p>Select a template to see available variables</p>
                        </div>
                    )}

                    <div className="variables-tips">
                        <h4>Tips</h4>
                        <ul>
                            <li>Variables are automatically replaced when emails are sent</li>
                            <li>Use HTML for styling (inline styles work best)</li>
                            <li>Test your template after making changes</li>
                            <li>Keep templates mobile-friendly</li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default EmailTemplateEditor;
