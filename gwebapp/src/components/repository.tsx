import * as React from "react"
import {MouseEventHandler} from "react"


interface RepositoryProps {
    id: number,
    name: string,
    timestamp?: number,
    onClick?: MouseEventHandler<HTMLDivElement>;
}

export default function Repository(props: RepositoryProps) {
    return <div onClick={props.onClick}>
        <div>
            Repository
        </div>
        <div>id: {props.id}</div>
        <div>name: {props.name}</div>
    </div>
}
