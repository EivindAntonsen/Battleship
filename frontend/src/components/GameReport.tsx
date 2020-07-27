import React from 'react';
import {GameReportDTO} from "../interface/GameReportDTO";
import PerformanceAnalysisComponent from "./PerformanceAnalysisComponent";

function GameReport(gameReportDTO: GameReportDTO) {

  const analysis = gameReportDTO.playerPerformance.map((performance) =>
    PerformanceAnalysisComponent(performance)
  );

  return (
    <div>
      <p>Game ID: {gameReportDTO.result.game.id}</p>
      <p>Winner: {gameReportDTO.result.winningPlayerId}</p>
    </div>
  );
}

export default GameReport;
