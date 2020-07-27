import React, {useState} from 'react';
import logo from './logo.svg';
import {useComponentWillMount} from "./hooks/useComponentWillMount";
import './App.css';
import axios, {AxiosResponse} from "axios";
import {GameReportDTO} from "./interface/GameReportDTO";

function App() {

  const [playerId, setPlayerId] = useState(0)

  useComponentWillMount(() => {
    axios.post("http://127.0.0.1:8098/game/play")
      .then((response: AxiosResponse<GameReportDTO>) => {
        setPlayerId(response.data.result.winningPlayerId);
      });
  });

  return (
    <div className="App">
      <header className="App-header">
        <img src={logo} className="App-logo" alt="logo"/>
        <p>
          Edit <code>src/App.tsx</code> and save to reload.
        </p>
        <a
          className="App-link"
          href="https://reactjs.org"
          target="_blank"
          rel="noopener noreferrer"
        >
          Learn React
        </a>

        <p>Winning player id: {playerId}</p>
      </header>
    </div>
  );
}

export default App;
