create table if not exists mogul (
    username text not null ,
    email text   null ,
    client_id  text not null ,
    unique (client_id, username)
) ;