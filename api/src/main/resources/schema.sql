create table if not exists mogul
(
    id serial primary key,
    username text not null ,
    client_id  text not null ,
    email text null,
    created timestamp not null default now(),
    unique (client_id, username)
);

create table if not exists managed_file_deletion_request
(

    id       serial primary key not null,
    mogul_id bigint             not null references mogul (id),
    bucket   text               not null,
    folder   text               not null,
    filename text               not null,
    storage_filename text not null,
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
    storage_filename text null,
    size     bigint             not null,
    content_type text not null,
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


create table if not exists publication
(
    id            serial primary key,
    mogul_id      bigint    not null references mogul (id),
    plugin        text      not null,
    created       timestamp not null default now(),
    published     timestamp null,
    context       text      not null,
    payload       text      not null,
    payload_class text      not null
);

create table if not exists podcast_episode
(
    podcast_id     bigint             not null references podcast (id),
    title          text               not null,
    description    text               not null,

    graphic        bigint             not null references managed_file (id),
    produced_graphic      bigint null references managed_file (id),

    interview      bigint             not null references managed_file (id),
    produced_interview    bigint null references managed_file (id),

    introduction   bigint             not null references managed_file (id),
    produced_introduction bigint null references managed_file (id),

    produced_audio bigint             null references managed_file (id),
    produced_audio_updated        timestamp null,
    produced_audio_assets_updated timestamp null,

    complete bool not null default false,

    id             serial primary key not null,
    created        timestamp          not null default now()
);
