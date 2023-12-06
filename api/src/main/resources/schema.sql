create table if not exists mogul
(
    id serial primary key,
    username text not null ,
    client_id  text not null ,
    email text null,
    unique (client_id, username)
) ;

create table if not exists podbean_account
(
    mogul_id      bigint,
    client_id     text not null,
    client_secret text not null,
    foreign key (mogul_id) references mogul (id)
);


create table if not exists podbean_episode
(
    revision             text                  not null,
    title                text,
    published            boolean default false not null,
    previously_published boolean default false not null,
    id                   varchar(255)          not null unique
);

-- todo change the ddl so that the podbean_episode_id is a foreign key pointing to the podbean_episode table, somehow
--  also the two things should have similar names: either podbean_podcast* or podbean_episode*


create table if not exists podcast_draft
(
    id          serial primary key,
    uid         varchar(255) unique,
    date        timestamp,
    title       text,
    description text,
    completed   bool ,
    mogul_id                bigint not null,

    picture_file_name text ,
    interview_file_name text ,
    intro_file_name text ,

    foreign key (mogul_id) references mogul (id)
);


create table if not exists podcast
(
    podbean_episode_id      varchar(255),
    id                      serial primary key,
    date                    timestamp,
    description             text,
    notes                   varchar(255),
    podbean_draft_created   timestamp,
    podbean_draft_published timestamp,
    podbean_media_uri       varchar(255),
    podbean_photo_uri       varchar(255),
    s3_audio_file_name      varchar(255),
    s3_audio_uri            varchar(255),
    s3_photo_file_name      varchar(255),
    s3_photo_uri            varchar(255),
    title                   text   not null,
    transcript              text,
    uid                     varchar(255) unique,
    mogul_id                bigint not null,
    foreign key (mogul_id) references mogul (id)
);

alter table podcast
    add column if not exists podbean_revision varchar(255) null;

