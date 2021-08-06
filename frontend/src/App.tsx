import React, {useReducer} from 'react';
import {BrowserRouter as Router, Route, Switch,} from "react-router-dom";

import './App.scss';

import ImageUpload from "./ImageUpload";
import FindImage from "./FindImage";
import Home from "./Home";
import {backendReducer} from "./backendReducer";

const initBackend = 'JAVA';

export const BackendContext = React.createContext([initBackend, null as any]);

const App: React.FunctionComponent = () => {
    const [backend, dispatch] = useReducer(backendReducer, initBackend);

    return (
        <BackendContext.Provider value={[backend, dispatch]}>
            <div className="App">
                <Router>
                    <Switch>
                        <Route exact path="/" component={Home}/>
                        <Route path="/register" component={ImageUpload}/>
                        <Route path="/find" component={FindImage}/>
                    </Switch>
                </Router>
            </div>
        </BackendContext.Provider>
    );
}

export default App;
