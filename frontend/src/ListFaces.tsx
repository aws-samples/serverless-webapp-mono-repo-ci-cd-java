import React, {useContext, useState, useEffect, useRef} from 'react';
import {Container, Card, CardMedia, CardContent, Typography, Grid} from "@material-ui/core";
import FormButtons from "./FormButtons";
import {BackendContext} from "./App";
import {GLOBAL_CONSTANTS} from "./GlobalConstants";

type Item = {
    imageUrl: string,
    fullName: string,
}

function ListFaces() {
    const [backend] = useContext(BackendContext);
    const isCancelled = useRef(false);
    const [items, setItems] = useState([]);
    const [message, setMessage] = useState<string>();
    const getFaceList = () => {
        fetch(`${GLOBAL_CONSTANTS.get(backend)?.LIST_FACES}`)
            .then(res => res.json())
            .then((result) => {
                    if (!isCancelled.current) {
                        setItems(result.body)
                    }
                },
                (error) => {
                    console.log(error);
                }
            ).catch(err => {
            setMessage('List faces failed');
        });
    };
    useEffect(() => {
        getFaceList();

        return () => {
            isCancelled.current = true;
        };
    }, []);
    return (
        <div>
            <FormButtons registerColor='default' findColor='default' listColor='primary'/>
            <Container maxWidth="md">

                <Grid
                    container
                    spacing={2}
                    direction="row"
                    justify="flex-start"
                    alignItems="flex-start">
                {
                    items.map((item : Item, index) =>
                <Card className="card_list">
                    <CardMedia
                        component="img"
                        height="140"
                        image={item.imageUrl}
                    />
                    <CardContent>
                        <Typography gutterBottom variant="h5" component="div">
                            {item.fullName}
                        </Typography>
                    </CardContent>
                </Card>)
                }

                </Grid>
            </Container>
            {/*<pre>{JSON.stringify(items)}</pre>*/}
        </div>
    );
}

export default ListFaces;