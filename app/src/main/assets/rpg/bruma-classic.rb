# Bruma classic / Ruby 1.8 compatibility.
# This runtime's one-shot Inflate.inflate corrupts unrelated strings in memory.
# An independent stream avoids that path and always releases its native state.
class << Zlib::Inflate
  def inflate(data)
    unless data.kind_of?(String)
      raise TypeError, 'expected a String' unless data.respond_to?(:to_str)
      data = data.to_str
      raise TypeError, 'to_str must return a String' unless data.kind_of?(String)
    end
    stream = new
    begin
      output = stream.inflate(data)
      output << stream.finish unless stream.finished?
      output
    ensure
      stream.close
    end
  end
end

