@groovy.transform.CompileStatic
String transform(Object payload) {
	"groovy-$payload"
}

transform(payload)
