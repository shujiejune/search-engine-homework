import json
import csv

# --- Configuration ---
GOOGLE_RESULTS_FILE = "Google_Result3.json"
SCRAPED_RESULTS_FILE = "hw1.json"
OUTPUT_CSV_FILE = "hw1.csv"

def normalize_url(url: str) -> str:
    """
    Normalizes a URL according to the assignment's rules.
    - Treat http and https as the same
    - Treat www.xyz.com and xyz.com as the same
    - Remove trailing slashes
    """
    if url.startswith('https://'):
        url = 'http://' + url[8:]
    if url.startswith('http://www.'):
        url = 'http://' + url[11:]
    if url.endswith('/'):
        url = url[:-1]
    return url

def calculate_spearman_coefficient(matches: list) -> float:
    """
    Calculates the Spearman coefficient based on a list of rank pairs.
    Each item in 'matches' should be a tuple (google_rank, other_rank).
    """
    n = len(matches)

    # Handle special cases as defined in the assignment
    if n == 0:
        return 0.0 # rho = 0 if no overlap
    
    if n == 1:
        google_rank, other_rank = matches[0]
        return 1.0 if google_rank == other_rank else 0.0
        
    # Calculate sum of d_i^2 for n > 1
    sum_d_squared = sum([(g_rank - o_rank) ** 2 for g_rank, o_rank in matches])
    
    # Apply the Spearman's rho formula
    rho = 1 - (6 * sum_d_squared) / (n * (n**2 - 1))
    
    return rho

def main():
    """
    Main function to analyze results and generate the CSV output.
    """
    try:
        with open(GOOGLE_RESULTS_FILE, 'r') as f:
            google_data = json.load(f)
        with open(SCRAPED_RESULTS_FILE, 'r') as f:
            scraped_data = json.load(f)
    except FileNotFoundError as e:
        print(f"Error: Could not find input file. Make sure '{GOOGLE_RESULTS_FILE}' and '{SCRAPED_RESULTS_FILE}' are present. Details: {e}")
        return

    # Prepare for calculating averages
    total_overlaps = 0
    total_percent_overlap = 0.0
    total_spearman = 0.0
    num_queries = len(google_data)

    with open(OUTPUT_CSV_FILE, 'w', newline='') as f:
        writer = csv.writer(f)
        # Write the header row for the CSV file
        writer.writerow(["Queries", "Number of Overlapping Results", "Percent Overlap", "Spearman Coefficient"])

        query_num = 1
        for query, google_urls in google_data.items():
            scraped_urls = scraped_data.get(query, [])
            
            # Normalize URLs for accurate comparison
            norm_google_urls = {normalize_url(url): i + 1 for i, url in enumerate(google_urls)}
            norm_scraped_urls = {normalize_url(url): i + 1 for i, url in enumerate(scraped_urls)}

            # Find overlapping URLs and their ranks
            overlapping_urls = set(norm_google_urls.keys()).intersection(set(norm_scraped_urls.keys()))
            
            num_overlaps = len(overlapping_urls)
            # Percent overlap is based on Google's 10 results
            percent_overlap = (num_overlaps / 10.0) * 100 
            
            rank_matches = []
            for url in overlapping_urls:
                google_rank = norm_google_urls[url]
                scraped_rank = norm_scraped_urls[url]
                rank_matches.append((google_rank, scraped_rank))

            spearman_coeff = calculate_spearman_coefficient(rank_matches)
            
            # Write the results for the current query to the CSV
            writer.writerow([f"Query {query_num}", num_overlaps, percent_overlap, spearman_coeff])

            # Add to totals for final average calculation
            total_overlaps += num_overlaps
            total_percent_overlap += percent_overlap
            total_spearman += spearman_coeff
            
            query_num += 1

        # Calculate and write the final averages row
        avg_overlaps = total_overlaps / num_queries if num_queries > 0 else 0
        avg_percent = total_percent_overlap / num_queries if num_queries > 0 else 0
        avg_spearman = total_spearman / num_queries if num_queries > 0 else 0
        
        writer.writerow(["Averages", avg_overlaps, avg_percent, avg_spearman])
    
    print(f"Analysis complete. Results saved to '{OUTPUT_CSV_FILE}'.")

if __name__ == '__main__':
    main()
