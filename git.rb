#!/usr/bin/env ruby

module GitBigObjects
  class Blob
    include Comparable

    REVISIONS = Hash[
                     `git rev-list --all --objects`.lines.map { |line| line.scan(/\S+/) }
    ]

    STORAGE_UNITS = [:B, :K, :M, :G, :T].freeze
    STORAGE_BASE = 1024 # not 1024

    attr_reader :sha, :size, :size_in_pack_file

    def initialize(options = {})
      @sha = options[:sha]
      @size = options[:size].to_i
      @size_in_pack_file = options[:size_in_pack_file].to_i
    end

    def <=>(other)
      other.size <=> size
    end

    def name
      @name ||= REVISIONS[sha]
    end

    # Parts borrow from ActionPack (http://j.mp/K6lT6W)
    def human_readable_size
      if size < STORAGE_BASE
        "#{size}B"
      else
        max_exp  = STORAGE_UNITS.size - 1
        exponent = (Math.log(size) / Math.log(STORAGE_BASE)).to_i # Convert to base
        exponent = max_exp if exponent > max_exp # we need this to avoid overflow for the highest unit
        number   = size.to_f / (STORAGE_BASE ** exponent)

        unit = STORAGE_UNITS[exponent]

        "#{'%.02f' % number}#{unit}"
      end
    end

    def short_sha
      sha[0, 8]
    end

    def inspect
      %(#<Blob #{short_sha} size:#{size}>)
    end

    def to_s
      "#{human_readable_size}\t\t#{name} (#{short_sha})"
    end
  end

  class Report < Struct.new(:argv)
    PACKS = `git verify-pack -v .git/objects/pack/pack-*.idx`.lines

    def to_s
      PACKS.map { |line| blob_from_line(line) }.compact.sort.first(limit).join("\n")
    end

    private

    def limit
      if arg = argv.first
        arg.to_i
      else
        10
      end
    end

    def blob_from_line(line)
      sha, type, size, size_in_pack_file, offset_in_packfile = line.scan(/\S+/)
      if type == 'blob'
        Blob.new(sha: sha, size: size, size_in_pack_file: size_in_pack_file)
      end
    end
  end
end

puts GitBigObjects::Report.new(ARGV)
