import sys
import os
import datetime
import argparse

my_parser = argparse.ArgumentParser(prog='updateVcfPath.py',
    usage='%(prog)s -v <vcf> -p <phenopacket> [-o <outdirectory]',
    description='Update VCF Path in phenopacket')
my_parser.add_argument('-v','--vcf', type=str, required=True, help='path to VCF file')
my_parser.add_argument('-p','--phenopacket', type=str, required=True, help='Path to phenopacket')
my_parser.add_argument('-o','--outdir',type=str, required=False, help='Path to output dir (default: cwd)')
my_parser.add_argument('-x','--extra',type=str, required=False, help='extra name part for output file')
args = my_parser.parse_args()

vcf_path = args.vcf
ppacket_path = args.phenopacket
extraname = args.extra
if args.outdir is not None:
    outdir = args.outdir
else:
    outdir = os.getcwd()

if not os.path.exists(vcf_path):
    print("[ERROR] Could not find vcf file at \"%s\"" % vcf_path)
    sys.exit(1)
if not os.path.exists(ppacket_path):
    print("[ERROR] Could not find Phenopacket file at \"%s\"" % ppacket_path)
    sys.exit(1)




def get_output_phenopacket_filename():
    base_phenopacket = os.path.basename(ppacket_path)
    if not base_phenopacket.endswith(".json"):
        print("[ERROR] phenopacket file must be JSON formated and end with .json")
        sys.exit(1)
    base_without_suffix = base_phenopacket[:-5]
    now = datetime.datetime.today().strftime ('%d-%b-%Y')
    if extraname is not None:
        return os.path.join(outdir, base_without_suffix + "-" + extraname + "-" + now + ".json")
    else:
        return os.path.join(outdir, base_without_suffix + "-" + now + ".json")


revised_ppacket_fname = get_output_phenopacket_filename()
print("[INFO] Will output updated phenopacket to %s" % revised_ppacket_fname)
fh = open(revised_ppacket_fname, 'wt')

"""
We need to put the updated VCF path into this line
"uri": "file://...",
note that we replace whatever path was originally provided.
"""
with open(ppacket_path) as f:
    for line in f:
        if line.strip().startswith("\"uri\""):
            myline = "    \"uri\": \"file:/%s\",\n" % vcf_path
            fh.write(myline)
        else:
            fh.write(line)
        
    
