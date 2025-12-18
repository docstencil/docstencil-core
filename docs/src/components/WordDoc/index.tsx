import React, { type ReactNode } from 'react';
import styles from './styles.module.css';

interface WordDocProps {
  filename: string;
  children: ReactNode;
  downloadUrl?: string;
}

function WordIcon(): React.JSX.Element {
  return (
    <svg className={styles.icon} viewBox="0 0 24 24" fill="#2B579A">
      <path d="M6 2h8l6 6v12a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z"/>
      <path d="M14 2v6h6" fill="none" stroke="#2B579A" strokeWidth="1"/>
      <text x="7" y="17" fontSize="7" fill="white" fontWeight="bold">W</text>
    </svg>
  );
}

function DownloadIcon(): React.JSX.Element {
  return (
    <svg className={styles.downloadIcon} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
      <polyline points="7 10 12 15 17 10"/>
      <line x1="12" y1="15" x2="12" y2="3"/>
    </svg>
  );
}

export default function WordDoc({ filename, children, downloadUrl }: WordDocProps): React.JSX.Element {
  return (
    <div className={styles.wordDoc}>
      <div className={styles.header}>
        <WordIcon />
        <span className={styles.filename}>{filename}</span>
        {downloadUrl && (
          <a href={downloadUrl} download className={styles.downloadButton} title="Download">
            <DownloadIcon />
          </a>
        )}
      </div>
        <div className={styles.content}>
          {children}
        </div>
    </div>
  );
}
