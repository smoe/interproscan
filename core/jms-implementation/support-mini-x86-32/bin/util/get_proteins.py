import requests, sys

def get_protein_accessions(input_file):
  protein_list = []
  with open(input_file, 'r') as inf:
    for line in inf:
      id_tokens =  line.split()
      for id in id_tokens:
        protein_list.append(id)
  return set(protein_list)

input_file = sys.argv[1].strip()
output_file = sys.argv[2].strip()

base_url = "https://www.ebi.ac.uk/proteins/api/proteins/"

protein_list = get_protein_accessions(input_file)
with  open(output_file, 'w') as outf:
  for protein_id in protein_list:
    requestURL = "https://www.ebi.ac.uk/proteins/api/proteins/" + protein_id
    r = requests.get(requestURL, headers={ "Accept" : "text/x-fasta"})
    if not r.ok:
      r.raise_for_status()
      sys.exit()

    responseBody = r.text
    outf.write(responseBody)

