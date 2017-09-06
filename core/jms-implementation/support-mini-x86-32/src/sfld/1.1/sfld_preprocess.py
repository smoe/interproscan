#!/usr/bin/env python3

from datetime import datetime, date, time
import sys
import re

def write_header(fn, f):
    f.write("## MSA feature annotation file\n")
    f.write("# Format version: 1.1\n")
    f.write("# MSA file: " + fn + "\n")
    now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    f.write("# Date " + now + "\n")

def parse_ft_line(s):
    ft_re = re.compile('#=GF FT\s+(\d+)\s*(.*)?')
    m = ft_re.match(s)
    if not m:
        return
    if len(m.groups()) > 1:
        return (int(m.group(1)), m.group(2))
    else:
        print(s)
        return (m.group(2), "")


def parse_msa(lines, f):
    if lines[0] != '# STOCKHOLM 1.0':
        return False
    sites = []
    gc_tags = set(['RF'])
    ft_re = re.compile('#=GF FT\s+(\d+)\s*(.*)?')
    gc_re = re.compile('#=GC\s+(\S+)\s+(.*)')
    features = []
    for line in lines:
        if line[:7] == '#=GF AC':
            ac = line.split()[-1]
        if line[:7] == '#=GF FT':
            (pos, desc) = parse_ft_line(line)
            sites.append([pos, desc])
        if line[:4] == '#=GC':
            m = gc_re.match(line)
            if m and m.group(1) not in gc_tags:
                i = 1
                ff = []
                for chr in m.group(2):
                    if chr != '.':
                        ff.append([chr, i])
                    i += 1
                features.append(ff)
    f.write("ACC %s %d %d\n" % (ac, len(sites), len(features)))
    for site in sorted(sites, key = lambda x: int(x[0])):
        f.write("SITE %d %s\n" % (site[0], site[1]))
        #for s in sorted(sites[desc], key = lambda x: int(x[0])):
    for feature in features:
        f.write("FEATURE ")
        for feature_site in feature:
            f.write(feature_site[0])
        f.write("\n")
    return True

if len(sys.argv) != 3:
    print("Usage")
    sys.exit(1)

lines = []
a = 0

with open(sys.argv[1], 'r') as msaf, open(sys.argv[2], 'w') as annot:
    write_header(sys.argv[1], annot)
    for line in msaf:
        line = line.rstrip()
        if len(line) == 0:
            continue
        if line == '//':
            a += parse_msa(lines, annot)
            lines = []
        else:
            lines.append(line)
#a += parse_msa(lines, annot)
#print (a)
