import {PerformanceAnalysis} from "./PerformanceAnalysis";

export interface GameReportDTO {
    result: Result
    playerPerformance: [PerformanceAnalysis]
}

interface Result {
    game: Game
    winningPlayerId: number
}

interface Game {
    id: number
    datetime: Date
    players: [Player]
    gameSeriesId: number | null
}

interface Player {
    id: number
    strategy: string
}
