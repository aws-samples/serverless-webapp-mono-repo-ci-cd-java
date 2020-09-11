import React from 'react';
import {BrowserRouter as Router, Route, Switch,} from "react-router-dom";

import './App.scss';

import ImageUpload from "./ImageUpload";
import FindImage from "./FindImage";
import Home from "./Home";

function App() {
  return (
    <div className="App">
        <Router>
            <Switch>
                <Route exact path="/" component={Home}/>
                <Route path="/register" component={ImageUpload}/>
                <Route path="/find" component={FindImage}/>
            </Switch>
        </Router>
    </div>
  );
}

export default App;
