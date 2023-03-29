require "java"
java_import 'java.util.Date'
#payload and headers a global variable
if payload
  payload = payload+" modified #{(defined? one) ? one : ''} #{(defined? two) ? two : ''} #{(defined? three) ? three : ''} #{Date.new}"
end
if headers
  headers.each {|key, value| puts "#{key} is #{value}" }
end
puts payload
payload
