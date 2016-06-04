require 'java'

class RubyHello
  include_class 'com.dturanski.test.jruby.Hello'
  def say
    "hello,world"
  end
end
RubyHello.new
