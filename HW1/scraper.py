import requests
import json
import time
import re
from random import randint
from bs4 import BeautifulSoup
from bs4.element import Tag

# --- Configuration ---
# URL and selector specific to the Ask search engine
# ASK_SEARCH_URL = "http://www.ask.com/web?q="
DUCKDUCKGO_SEARCH_URL = "https://duckduckgo.com/html/?q="

# The selector for an organic result link, as specified in the assignment
DUCKDUCKGO_SELECTOR = ["a", {"class": "result__a"}]

# User agent to mimic a real browser visit
USER_AGENT = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36'
}

# Input file containing 100 queries, one per line
QUERIES_FILE = "100QueriesSet4.txt"

# Output file for scraped results
OUTPUT_JSON_FILE = "hw1.json"

def scrape_search(query: str) -> list:
    """
    Scrapes the Ask.com search results for a given query and returns the top 10 URLs.
    """
    search_url = DUCKDUCKGO_SEARCH_URL + query.replace(" ", "+")
    results = []
    
    retries = 3
    for i in range(retries):
        try:
            response = requests.get(search_url, headers=USER_AGENT, timeout=20) # Added timeout
            response.raise_for_status()

            soup = BeautifulSoup(response.text, 'html.parser')
            
            link_tags = soup.find_all(DUCKDUCKGO_SELECTOR[0], attrs=DUCKDUCKGO_SELECTOR[1])

            for link_tag in link_tags:
                if link_tag.find_parent('div', class_='result--ad'):
                    continue

                if isinstance(link_tag, Tag):
                    link = link_tag.get('href')
                    if isinstance(link, str) and link.startswith('http') and link not in results:
                        results.append(link)
                
                if len(results) >= 10:
                    break
            
            # If the scrape was successful, break out of the retry loop
            return results
            
        except requests.exceptions.RequestException as e:
            print(f"  > Network error on attempt {i+1}/{retries}: {e}")
            if i < retries - 1:
                wait_time = (i + 1) * 60  # Wait for 1 min, then 2 mins, etc.
                print(f"  > Waiting for {wait_time} seconds before retrying...")
                time.sleep(wait_time)
            else:
                print(f"  > Max retries exceeded for query. Moving to the next one.")
                return [] # Return empty list after final failure
        except Exception as e:
            print(f"  > An unexpected error occurred: {e}")
            return [] # Return empty for other unexpected errors

    return results

def main():
    """
    Main function to read queries, scrape results, and save to a JSON file.
    """
    scraped_data = {}
    
    # Read queries from the input file
    try:
        with open(QUERIES_FILE, 'r') as f:
            queries = [line.strip() for line in f if line.strip()]
    except FileNotFoundError:
        print(f"Error: The query file '{QUERIES_FILE}' was not found.")
        return

    print(f"Starting to scrape {len(queries)} queries from Ask.com...")

    for i, query in enumerate(queries):
        if query in scraped_data:
            continue

        print(f"Scraping query {i+1}/{len(queries)}: '{query}'")
        
        results = scrape_search(query)
        scraped_data[query] = results
        
        # IMPORTANT: Wait with a random delay to avoid being blocked
        # The assignment specifies a random delay between 10 and 100 seconds.
        delay = randint(10, 100)
        print(f"  > Found {len(results)} results. Waiting for {delay} seconds...")
        time.sleep(delay)

    # Save the final dictionary to a JSON file
    with open(OUTPUT_JSON_FILE, 'w') as f:
        json.dump(scraped_data, f, indent=4)

    print(f"\nScraping complete. Results saved to '{OUTPUT_JSON_FILE}'.")


if __name__ == '__main__':
    main()
