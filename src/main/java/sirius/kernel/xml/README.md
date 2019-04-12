# XML Handling

Provides ways of reading and writing XML data.

## Reading XML

The [XMLReader](XMLReader.java) provides a convenient way for processing large XML files. A set of handlers can
be registered on node names. The reader will then parse given data using a memory efficient SAX parser. Once a
node with a regisered handler is found, the sub DOM is parsed and sent to the handler as [StructuredNode](StructuredNode.java).

Using this approach, very large XML files can be processed with almost constant memory overhead.

For smaller XML files [XMLStructuredInput](XMLStructuredInput.java) can be used to convert a whole
document into a single [StructuredNode](StructuredNode.java).

## Writing XML

With the help of [XMLStructuredOutput](XMLStructuredOutput.java) or a [XMLGenerator](XMLGenerator.java) XML data
can be generated quite easily.

## Abstraction

Note that both **XMLStructuredInput** and **XMLStructuredOutput** are specified by the interfaces
[StructuredInput](StructuredInput.java) and [StructuredOutput](StructuredOutput.java). This is used
by [sirius-web](https://github.com/scireum/sirius-web) which provides implementations for these interfaces
to read and write JSON data. This way services can be created which are capable of reading and
writing both.

## Webservices

To call XML based REST services the [XMLCall](XMLCall.java) can be used which sends and/or
receives XML data from an URL endpoint. Note that the base facility which provides a thin abstraction
above HttpURLConnection is also made available as [Outcall](Outcall.java).
