import React, { useState } from 'react';
import '../../styles/MFA.css';

function BackupCodesDisplay({ codes }) {
    const [copied, setCopied] = useState(false);

    const handleCopyAll = () => {
        const codesText = codes.join('\n');
        navigator.clipboard.writeText(codesText);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    const handleDownload = () => {
        const codesText = codes.join('\n');
        const blob = new Blob([codesText], { type: 'text/plain' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `backup-codes-${new Date().toISOString().split('T')[0]}.txt`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    };

    const handlePrint = () => {
        const printWindow = window.open('', '_blank');
        const codesHtml = codes.map((code, index) =>
            `<div style="padding: 8px; font-family: monospace; font-size: 18px;">${index + 1}. ${code}</div>`
        ).join('');

        printWindow.document.write(`
            <!DOCTYPE html>
            <html>
                <head>
                    <title>Backup Codes</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            padding: 40px;
                            max-width: 600px;
                            margin: 0 auto;
                        }
                        h1 {
                            color: #333;
                            border-bottom: 2px solid #667eea;
                            padding-bottom: 10px;
                        }
                        .warning {
                            background-color: #fff3cd;
                            border: 1px solid #ffc107;
                            border-radius: 5px;
                            padding: 15px;
                            margin: 20px 0;
                        }
                        .codes-container {
                            margin-top: 30px;
                        }
                        .date {
                            color: #666;
                            font-size: 14px;
                            margin-top: 30px;
                        }
                        @media print {
                            body { padding: 20px; }
                        }
                    </style>
                </head>
                <body>
                    <h1>AI NEXUS HUB - Backup Codes</h1>
                    <div class="warning">
                        <strong>Important:</strong> Store these codes in a secure location.
                        Each code can only be used once.
                    </div>
                    <div class="codes-container">
                        ${codesHtml}
                    </div>
                    <div class="date">
                        Generated: ${new Date().toLocaleString()}
                    </div>
                </body>
            </html>
        `);
        printWindow.document.close();
        printWindow.print();
    };

    if (!codes || codes.length === 0) {
        return (
            <div className="backup-codes-empty">
                No backup codes available
            </div>
        );
    }

    return (
        <div className="backup-codes-container">
            <div className="backup-codes-grid">
                {codes.map((code, index) => (
                    <div key={index} className="backup-code-item">
                        <span className="backup-code-number">{index + 1}.</span>
                        <code className="backup-code">{code}</code>
                    </div>
                ))}
            </div>

            <div className="backup-codes-actions">
                <button
                    type="button"
                    className="btn-action"
                    onClick={handleCopyAll}
                >
                    {copied ? 'Copied!' : 'Copy All'}
                </button>
                <button
                    type="button"
                    className="btn-action"
                    onClick={handleDownload}
                >
                    Download
                </button>
                <button
                    type="button"
                    className="btn-action"
                    onClick={handlePrint}
                >
                    Print
                </button>
            </div>
        </div>
    );
}

export default BackupCodesDisplay;
