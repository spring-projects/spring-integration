require "java"
java_import 'java.util.Date'
#payload and headers a global variable
if payload
  payload = payload+" modified #{Date.new}"
end
puts payload
if headers
  headers.each {|key, value| puts "#{key} is #{value}" }
end
puts "#{$one} #{$two} #{$three}"
payload
