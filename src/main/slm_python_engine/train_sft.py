import os


MAX_THREADS = "4"
os.environ["OMP_NUM_THREADS"] = MAX_THREADS
os.environ["MKL_NUM_THREADS"] = MAX_THREADS
os.environ["OPENBLAS_NUM_THREADS"] = MAX_THREADS
os.environ["NUMEXPR_NUM_THREADS"] = MAX_THREADS

import torch
from dotenv import load_dotenv
from datasets import load_dataset
from trl import SFTTrainer, SFTConfig

from setup_lora import get_lora_model_and_tokenizer

# Load environment variables from the root .env file
load_dotenv()

# Output directory for saved LoRA adapters
OUTPUT_DIR = "./slm_lora_adapter"
DATASET_PATH = "dataset.jsonl"

def train():
    # 1. Verify Dataset Availability
    if not os.path.exists(DATASET_PATH):
        raise FileNotFoundError(
            f"Dataset file '{DATASET_PATH}' not found. Please run generate_dataset.py first!"
        )

    print("Loading synthetic ChatML function-calling dataset...")
    dataset = load_dataset("json", data_files=DATASET_PATH, split="train")

    # 2. Load Base Model wrapped with LoRA Adapters & Tokenizer
    model, tokenizer = get_lora_model_and_tokenizer()

    # 3. Configure Fine-Tuning Arguments explicitly configured for CPU
    sft_config = SFTConfig(
        output_dir=OUTPUT_DIR,
        dataset_text_field="messages",
        max_length=512,
        num_train_epochs=3,                  # 3 epochs for fast CPU execution
        per_device_train_batch_size=4,
        gradient_accumulation_steps=2,
        learning_rate=2e-4,                  # Standard LoRA learning rate
        lr_scheduler_type="cosine",          # Cosine decay for smooth convergence
        warmup_steps=10,
        optim="adamw_torch",                 # Standard PyTorch AdamW CPU optimizer
        logging_steps=10,
        save_strategy="epoch",
        use_cpu=True,                        # Explicit CPU execution flag
        fp16=False,                          # Disable GPU half-precision
        bf16=False,                          # Disable bfloat16 for CPU compatibility
        report_to="none"                     # Disables external tracking overhead
    )

    # 4. Initialize SFTTrainer (Removed 'tokenizer' argument as handled by TRL base trainer)
    trainer = SFTTrainer(
        model=model,
        train_dataset=dataset,
        args=sft_config
    )

    # 5. Execute Training
    print("\nStarting Supervised Fine-Tuning (SFT) on CPU...")
    trainer.train()

    # 6. Save the Trained LoRA Adapter Weights & Tokenizer
    print(f"\nTraining complete! Saving LoRA adapter to '{OUTPUT_DIR}'...")
    trainer.model.save_pretrained(OUTPUT_DIR)
    tokenizer.save_pretrained(OUTPUT_DIR)
    print("Adapter successfully saved!")

if __name__ == "__main__":
    train()