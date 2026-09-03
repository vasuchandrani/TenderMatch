import React from 'react';

const MatchProgressBar = ({
  matchCount = 0,
  unmatchCount = 0,
  aiErrCount = 0,
  total = 0,
  height = 8,
}) => {
  const tot = total > 0 ? total : matchCount + unmatchCount + aiErrCount;
  if (tot === 0) {
    return (
      <div
        data-testid="match-progress-bar"
        style={{
          width: '100%',
          height: `${height}px`,
          backgroundColor: '#e2e8f0',
          borderRadius: '4px',
        }}
      />
    );
  }

  const matchPct = (matchCount / tot) * 100;
  const unmatchPct = (unmatchCount / tot) * 100;
  const aiErrPct = (aiErrCount / tot) * 100;

  return (
    <div
      data-testid="match-progress-bar"
      style={{
        width: '100%',
        height: `${height}px`,
        backgroundColor: '#e2e8f0',
        borderRadius: '4px',
        overflow: 'hidden',
        display: 'flex',
      }}
    >
      <div
        style={{
          width: `${matchPct}%`,
          backgroundColor: '#10b981',
          transition: 'width 0.4s ease',
        }}
        title={`Match: ${matchCount} (${Math.round(matchPct)}%)`}
      />
      <div
        style={{
          width: `${unmatchPct}%`,
          backgroundColor: '#ef4444',
          transition: 'width 0.4s ease',
        }}
        title={`Unmatch: ${unmatchCount} (${Math.round(unmatchPct)}%)`}
      />
      <div
        style={{
          width: `${aiErrPct}%`,
          backgroundColor: '#f59e0b',
          transition: 'width 0.4s ease',
        }}
        title={`AI Err: ${aiErrCount} (${Math.round(aiErrPct)}%)`}
      />
    </div>
  );
};

export default MatchProgressBar;
