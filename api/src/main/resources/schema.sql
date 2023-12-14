create table if not exists mogul
(
    id serial primary key,
    username text not null ,
    client_id  text not null ,
    email text null,
    created timestamp not null default now(),
    unique (client_id, username)
) ;



create table if not exists managed_file_deletion_request
(

    id       serial primary key not null,
    mogul_id bigint             not null references mogul (id),
    bucket   text               not null,
    folder   text               not null,
    filename text               not null,
    deleted  bool               not null default false,
    created  timestamp          not null default now()
);

create table if not exists managed_file
(
    mogul_id bigint             not null references mogul (id),
    created  timestamp          not null default now(),
    id       serial primary key not null,
    bucket   text               not null,
    folder   text               not null,
    filename text               not null,
    size     bigint             not null,
    written  bool               not null default false
);

create table if not exists settings
(
    mogul_id bigint    not null references mogul (id),
    key      text   not null,
    "value"  text   not null,
    category text   not null,
    created  timestamp not null default now(),
    unique (mogul_id, category, key)
) ;

create table if not exists podcast
(
    mogul_id bigint             not null references mogul (id),
    title    text               not null,
    created  timestamp          not null default now(),
    id       serial primary key not null,
    unique (mogul_id, title)

);

create table if not exists podcast_episode
(
    podcast_id     bigint             not null references podcast (id),
    title          text               not null,
    description    text               not null,

    graphic        bigint             not null references managed_file (id),
    interview      bigint             not null references managed_file (id),
    introduction   bigint             not null references managed_file (id),
    produced_audio bigint             null references managed_file (id),

    id             serial primary key not null,
    created        timestamp          not null default now()
);


/*
create table if not exists podcast
(
    podbean_episode_id varchar(255),
    id                 serial primary key,
    date               timestamp,
    description        text,
    notes              varchar(255),
    podbean_media_uri  varchar(255),
    podbean_photo_uri  varchar(255),
    podbean_permalink_uri varchar(255),
    podbean_player_uri    varchar(255),
    duration           bigint,
    s3_audio_file_name varchar(255),
    s3_audio_uri       varchar(255),
    s3_photo_file_name varchar(255),
    s3_photo_uri       varchar(255),
    title              text   not null,
    deleted            bool   not null default false,
    transcript         text,
    uid                varchar(255) unique,
    mogul_id           bigint not null,
    needs_promotion    bool   not null default false,
    unique (mogul_id, title),
    foreign key (mogul_id) references mogul (id)
);


create table if not exists podcast_draft
(
    id                  serial primary key,
    uid                 varchar(255) unique,
    date                timestamp,
    title               text,
    description         text,
    completed           bool not null  default false,
    mogul_id            bigint not null,

    picture_file_name   text,
    interview_file_name text,
    intro_file_name     text,
    podcast_id          bigint null,
    foreign key (podcast_id) references podcast (id),
    foreign key (mogul_id) references mogul (id)
);


create table if not exists podbean_publication_tracker
(
    id                serial primary key,
    node_id           text      not null,
    mogul_id          int       not null,
    continue_tracking bool      not null,
    podcast_id        int       not null,
    started           timestamp not null,
    stopped           timestamp,
    unique (podcast_id)
);
*/