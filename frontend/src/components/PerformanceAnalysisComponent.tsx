import React from 'react';
import {PerformanceAnalysis} from "../interface/PerformanceAnalysis";

function PerformanceAnalysisComponent(performanceAnalysis: PerformanceAnalysis) {
  return (
    <div>
      <p>Player id: {performanceAnalysis.playerId}</p>
      <ul>
        <li>Shots: {performanceAnalysis.shots}</li>
        <li>Hits: {performanceAnalysis.hits}</li>
        <li>Misses: {performanceAnalysis.misses}</li>
        <li>Hitrate: {performanceAnalysis.hitRate}</li>
      </ul>
    </div>
  );
}

export default PerformanceAnalysisComponent;
