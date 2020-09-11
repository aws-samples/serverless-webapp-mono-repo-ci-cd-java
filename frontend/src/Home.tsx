import React from 'react';
import {Container} from "@material-ui/core";
import FormButtons from "./FormButtons";

function Home() {
    return (
        <div>
            <FormButtons registerColor='default' findColor='default'/>
            <Container maxWidth="md">
                {/*<div className='home-container'>
                    hello
                </div>*/}
            </Container>
        </div>
    );
}

export default Home;