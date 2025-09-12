import requests
import json
import time
from random import randint
from bs4 import BeautifulSoup
from bs4.element import Tag

# --- Configuration ---
# URL and selector specific to the Ask search engine
ASK_SEARCH_URL = "http://www.ask.com/web?q="
# The selector for Ask finds the title block of a search result.
ASK_SELECTOR = ["div", {"class": "PartialSearchResults-item-title"}] 

# User agent to mimic a real browser visit
USER_AGENT = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36'
}

# Input file containing 100 queries, one per line
QUERIES_FILE = "100QueriesSet3.txt"

# Output file for scraped results
OUTPUT_JSON_FILE = "hw1.json"

def scrape_ask_search(query: str) -> list:
    """
    Scrapes the Ask.com search results for a given query and returns the top 10 URLs.
    """
    search_url = ASK_SEARCH_URL + query.replace(" ", "+")
    results = []
    
    try:
        response = requests.get(search_url, headers=USER_AGENT)
        response.raise_for_status() # Raises an exception for bad status codes

        soup = BeautifulSoup(response.text, 'html.parser')
        
        # Find all result containers using the selector
        raw_results = soup.find_all(ASK_SELECTOR[0], attrs=ASK_SELECTOR[1])

        for result in raw_results:
            # The link is in an 'a' tag within the selected div.
            if isinstance(result, Tag):
                link_tag = result.find('a')
                if isinstance(link_tag, Tag):
                    link = link_tag.get('href')
                    if isinstance(link, str):
                        if link.startswith('http') and link not in results:
                            results.append(link)
            
            # Only collect the top 10 results
            if len(results) >= 10:
                break
                
    except requests.exceptions.RequestException as e:
        print(f"Error scraping for query '{query}': {e}")
        # If a query fails, return an empty list or handle it as needed.
        return []

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
        print(f"Scraping query {i+1}/{len(queries)}: '{query}'")
        
        results = scrape_ask_search(query)
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
