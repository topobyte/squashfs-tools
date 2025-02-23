# SquashFs Tools

This project provides utilities to create, inspect and extract squashfs
filesystems.

It consists of a library that you can use in your own projects and a CLI
application.

## History

This is a fork of [ccondit-target/squashfs-tools](https://github.com/ccondit-target/squashfs-tools)
that has been modified and extended.

## Setup

In order to run any of the executables of this project, first build it:

    ./gradlew clean createRuntime

## Usage

To convert a directory to squashfs:

    ./scripts/squashfs-convert-directory <directory> <squashfs-file>

To convert a Docker tar.gz layer to squashfs:

    ./scripts/squashfs-convert-tar-gz <tar-gz-file> <squashfs-file>

To dump the raw content of a squashfs file:

    ./scripts/squashfs-fsck <squashfs-file>

To extract the contents of a squashfs file:

    ./scripts/squashfs-extract <squashfs-file> <directory>
