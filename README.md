# ZIO Process

[![CircleCI](https://circleci.com/gh/zio/zio-process/tree/master.svg?style=svg)](https://circleci.com/gh/zio/zio-process/tree/master)

## Getting Started

To use `zio-process`, add the following line in your `build.sbt` file:

```scala
libraryDependencies += "dev.zio" %% "zio-process" % "0.1.0"
```

## Usage

Build a description of a command:

```scala
val command = Command("cat", "file.txt")
```

`command.run` will return a handle to the process as `RIO[Blocking, Process]`. Alternatively, instead of flat-mapping
and calling methods on `Process`, there are convenience methods on `Command` itself for some common operations:

```scala
// Return output as a list of lines: RIO[Blocking, List[String]]
command.lines

// Return output as a stream of lines: ZStream[Blocking, Throwable, String]
// Particularly useful when dealing with large files and so on as to not use an unbounded amount of memory.
command.linesStream

// Return entire output as string: RIO[Blocking, String]
command.string

// Return only the exit code: RIO[Blocking, Int]
command.exitCode
```

### Piping

You can pipe the output of one process as the input to another. For example, if you want to return a list of all
running Java process IDs, you can do the following:

```scala
for {
  processes     <- Command("ps", "-ef").stream
  javaProcesses <- Command("grep", "java").stdin(ProcessInput.fromStreamChunk(processes)).stream
  processIds    <- Command("awk", "{print $2}").stdin(ProcessInput.fromStreamChunk(javaProcesses)).lines
} yield processIds
```

Rather than connecting the outputs and inputs manually in this way, you can use the `|` operator (or its named
equivalent, `pipe`) like so:

```scala
(Command("ps", "-ef") | Command("grep", "java") | Command("awk", "{print $2}")).lines
```

### Inheriting I/O

If you'd like to run a process and handle its input/output in the current process, you can inherit its I/O. For example,
running the Scala REPL:

```scala
Command("scala").inheritIO.exitCode
```

### Providing environment variables

```scala
Command("java", "-version").env(Map("JAVA_HOME" -> javaHome)).string
```

### Specifying the working directory

```scala
Command("ls").workingDirectory(new File("/")).lines
```