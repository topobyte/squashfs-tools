SquashFs Tools
==============

To convert a directory to squashfs:

```
./scripts/squashfs-convert-directory <directory> <squashfs-file>
```

To convert a Docker tar.gz layer to squashfs:

```
./scripts/squashfs-convert-tar-gz <tar-gz-file> <squashfs-file>
```

To dump the raw content of a squashfs file:

```
./scripts/squashfs-fsck <squashfs-file>
```
